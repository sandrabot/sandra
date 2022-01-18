/*
 * Copyright 2017-2022 Avery Carroll and Logan Devecka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandrabot.sandra.api

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.utils.httpClient
import com.sandrabot.sandra.utils.string
import com.sandrabot.sandra.utils.toJson
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory
import java.io.File
import java.util.zip.ZipFile
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.set
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * This class receives and processes calls to the Sandra API.
 */
class SandraAPI(private val sandra: Sandra, private val port: Int) {

    private var deploymentAllowed = false
    private val secretKey = SecretKeySpec(sandra.credentials.githubSecret.toByteArray(), "HmacSHA256")
    private val deployScope = CoroutineScope(EmptyCoroutineContext + Dispatchers.IO)
    private val api: Javalin = Javalin.create { config ->
        config.defaultContentType = "application/json"
        config.showJavalinBanner = false
        config.prefer405over404 = true
        if (sandra.development) config.enableDevLogging()
    }

    init {

        api.before {
            logger.info("Received ${it.method()} ${it.url()} from ${it.ip()} ${it.userAgent()}")
            sandra.statistics.incrementRequestCount()
        }

        api.routes {
            path("/api/v1") {
                // A path is used here because more routes will be added in the future
                get("/status", ::status)
                post("/deploy", ::deploy)
            }
        }

        api.error(404) { createError(it, it.status(), "Resource not found") }
        api.error(405) { createError(it, it.status(), "Method not allowed") }
        api.error(500) { createError(it, it.status(), "Internal server error") }

        api.exception(ForbiddenResponse::class.java) { e, context -> createError(context, e.status, e.message!!) }
        api.exception(BadGatewayResponse::class.java) { e, context -> createError(context, e.status, e.message!!) }
        api.exception(BadRequestResponse::class.java) { e, context -> createError(context, e.status, e.message!!) }
        api.exception(UnauthorizedResponse::class.java) { e, context -> createError(context, e.status, e.message!!) }
        api.exception(ServiceUnavailableResponse::class.java) { e, context -> createError(context, e.status, e.message!!) }

    }

    fun allowDeployment() {
        deploymentAllowed = true
    }

    fun start() {
        api.start(port)
    }

    fun shutdown() {
        api.stop()
    }

    /* Factory Methods */

    private fun createError(context: Context, code: Int, message: String) {
        createResponse(context) {
            it["success"] = false
            it["message"] = message
            it["code"] = code
        }
        context.status(code)
    }

    private fun createResponse(
        context: Context, code: Int = 200, handler: ((MutableMap<String, Any>) -> Unit)? = null
    ) {
        val response = mutableMapOf<String, Any>()
        response["success"] = true
        if (handler != null) handler(response)
        response["version"] = Constants.VERSION
        // Parse the map into json and set the response body
        context.status(code).result(response.toJson())
    }

    /* Route Handlers */

    private fun status(context: Context) {
        createResponse(context) {
            it["ping"] = sandra.shards.averageGatewayPing
            it["guilds"] = sandra.shards.guildCache.size()
            it["requests"] = sandra.statistics.requestCount
            val runtime = Runtime.getRuntime()
            it["memory"] = (runtime.totalMemory() - runtime.freeMemory()) shr 20
            it["uptime"] = (System.currentTimeMillis() - sandra.statistics.startTime) / 1000
        }
    }

    private fun deploy(context: Context) {
        // Ensure that a deployment was deliberately expected to prevent unauthorized deployments
        if (!deploymentAllowed) {
            logger.warn("Deployment endpoint was called and denied by deploymentAllowed = false")
            throw ServiceUnavailableResponse()
        }
        // Validate that this request is from GitHub
        val mac = Mac.getInstance("HmacSHA256").apply { init(secretKey) }
        val signature = "sha256=" + mac.doFinal(context.bodyAsBytes()).joinToString("") { "%02x".format(it) }
        if ("X-Hub-Signature-256" !in context.headerMap()) throw UnauthorizedResponse("Missing signature header")
        if (context.header("X-Hub-Signature-256") != signature) throw ForbiddenResponse("Invalid signature header")
        val runId = context.formParam("runId")?.toIntOrNull() ?: throw BadRequestResponse("Missing runId parameter")
        logger.info("Verified request to deploy update from GitHub action $runId")
        deployScope.launch { doDeploy(runId) }
        createResponse(context, 202) {
            it["message"] = "Deployment created"
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @OptIn(ExperimentalTime::class)
    private suspend fun doDeploy(runId: Int) {
        val latestZip = File("latest-build.zip")
        // Time how long it takes to download the file
        val duration = measureTime {
            val githubToken = sandra.credentials.githubToken
            // Retrieve the list of artifacts that were generated by this run
            val actionArtifacts = httpClient.get<HttpResponse>(
                "https://api.github.com/repos/sandrabot/sandra/actions/runs/$runId/artifacts"
            ) { header("Authorization", "Bearer $githubToken") }
            if (actionArtifacts.status != HttpStatusCode.OK) throw BadGatewayResponse("Failed to retrieve artifacts")
            // Find the first artifact with the name "build-libs" which contains our binaries
            val artifacts = Json.decodeFromString<JsonObject>(actionArtifacts.readText())["artifacts"]!!.jsonArray
            val downloadUrl = artifacts.map { it.jsonObject }.first {
                it.string("name") == "build-libs"
            }.string("archive_download_url")!!
            logger.info("Downloading artifacts from $downloadUrl")
            // Download the binaries from the download url provided by GitHub
            val download = httpClient.get<HttpResponse>(downloadUrl) { header("Authorization", "Bearer $githubToken") }
            if (download.status != HttpStatusCode.OK) throw BadGatewayResponse("Failed to download artifacts")
            download.content.copyAndClose(latestZip.writeChannel())
        }
        val megaBytes = latestZip.length() shr 20
        logger.info("Finished downloading artifact in $duration with size ${megaBytes}MB to ${latestZip.absolutePath}")
        // Extract only the shadow jar and rename it to something constant
        ZipFile(latestZip).use { zip ->
            val regex = Regex("""sandra-\d\.\d\.\d(?:-SNAPSHOT)?-all\.jar""")
            val entry = zip.entries().toList().first { it.name.matches(regex) }
            zip.getInputStream(entry).use { File("sandra-latest.jar").outputStream().use(it::copyTo) }
            logger.info("Selected and extracted binary ${entry.name} for installation")
        }
        latestZip.delete()
        // If sandra-latest.jar exists it will replace the main jar
        sandra.shutdown(restart = true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SandraAPI::class.java)
    }

}
