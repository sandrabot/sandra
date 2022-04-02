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
import com.sandrabot.sandra.utils.*
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/**
 * This class receives and processes calls to the Sandra API.
 */
class SandraAPI(private val sandra: Sandra, private val port: Int) {

    private var deploymentAllowed = false
    private val secretKey = SecretKeySpec(sandra.credentials.githubSecret.toByteArray(), "HmacSHA256")
    private val api: Javalin = Javalin.create { config ->
        config.defaultContentType = "application/json"
        config.showJavalinBanner = false
        config.prefer405over404 = true
        if (sandra.development) config.enableDevLogging()
        config.contextResolvers { resolvers ->
            resolvers.ip = { ctx ->
                // If the Forwarded header exists, use the for field to determine the original ip address
                ctx.header("Forwarded")?.substringAfter("for=")?.substringBefore(",")?.substringBefore(";")
                    ?: ctx.req.remoteAddr
            }
        }
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

        api.exception(HttpResponseException::class.java) { e, context ->
            logger.info("Handler threw response exception while processing request", e)
            createError(context, e.status, e.message!!)
        }

    }

    @Suppress("unused")
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

    private fun status(context: Context) = createResponse(context) {
        it["ping"] = sandra.shards.averageGatewayPing
        it["guilds"] = sandra.shards.guildCache.size()
        it["requests"] = sandra.statistics.requestCount
        val runtime = Runtime.getRuntime()
        it["memory"] = (runtime.totalMemory() - runtime.freeMemory()) shr 20
        it["uptime"] = (System.currentTimeMillis() - sandra.statistics.startTime) / 1000
    }

    @OptIn(ExperimentalTime::class)
    private fun deploy(context: Context) {
        // Ensure that the deployment was properly authorized
        if (!deploymentAllowed) {
            logger.warn("Deployment endpoint was called and denied by deploymentAllowed = false")
            throw ServiceUnavailableResponse()
        }

        // Validate that this request is from GitHub
        val mac = Mac.getInstance("HmacSHA256").apply { init(secretKey) }
        val signature = "sha256=" + mac.doFinal(context.bodyAsBytes()).joinToString("") { "%02x".format(it) }
        if ("X-Hub-Signature-256" !in context.headerMap()) throw UnauthorizedResponse("Missing signature header")
        if (context.header("X-Hub-Signature-256") != signature) throw ForbiddenResponse("Invalid signature header")
        val runId = context.formParam("runId")?.toLongOrNull() ?: throw BadRequestResponse("Missing runId parameter")
        logger.info("Verified request to deploy update from GitHub action $runId")

        val latestBuildZip = File("latest-build.zip")
        runBlocking(Dispatchers.IO) {
            // Retrieve the list of artifacts that were generated by this run
            val actionArtifacts = try {
                // This endpoint does not require any authorization
                httpClient.get<HttpResponse>("https://api.github.com/repos/sandrabot/sandra/actions/runs/$runId/artifacts")
            } catch (t: Throwable) {
                logger.error("Failed to retrieve artifacts from GitHub", t)
                throw BadGatewayResponse("Failed to retrieve artifacts")
            }

            // Find the first artifact with the name "build-libs" which contains our binaries
            val artifactResponse = Json.decodeFromString<JsonObject>(actionArtifacts.readText())
            // Throw if the total_count is null or already a zero
            if ((artifactResponse.int("total_count") ?: 0) == 0) throw BadGatewayResponse("Job has no artifacts")
            val libsArtifact = artifactResponse["artifacts"]!!.jsonArray.map { it.jsonObject }.find {
                it.string("name") == "build-libs"
            } ?: throw BadGatewayResponse("Missing build-libs artifact")
            // Make sure that the artifacts are not expired, just to be safe
            if (libsArtifact.boolean("expired") == true) throw BadRequestResponse("Artifact is expired")
            val libsSizeBytes = libsArtifact.long("size_in_bytes") ?: throw BadGatewayResponse("Missing size_in_bytes")
            val downloadUrl = libsArtifact.string("archive_download_url") ?: throw BadGatewayResponse("Missing archive_download_url")
            logger.info("Downloading build-libs with size $libsSizeBytes from $downloadUrl")

            // Time how long it takes to download the zip file from GitHub
            val timedDownload = measureTimedValue {
                val download = try {
                    // This endpoint *does* require authorization with an actions scope
                    httpClient.get<HttpResponse>(downloadUrl) {
                        header("Authorization", "Bearer ${sandra.credentials.githubToken}")
                    }
                } catch (t: Throwable) {
                    logger.error("Failed to download artifacts from GitHub", t)
                    throw BadGatewayResponse("Failed to download artifacts")
                }
                download.content.copyAndClose(latestBuildZip.writeChannel())
            }

            // Finish processing the downloaded zip file from GitHub
            logger.info("Finished downloading in ${timedDownload.duration} with size ${timedDownload.value} to ${latestBuildZip.absolutePath}")
            ZipFile(latestBuildZip).use { zip ->
                // Extract only the shadow jar and rename it to something more constant
                val entry = zip.entries().toList().first { it.name.endsWith("-all.jar") }
                zip.getInputStream(entry).use { File("sandra-latest.jar").outputStream().use(it::copyTo) }
                logger.info("Selected and extracted binary ${entry.name} for installation")
            }
            // The zip is no longer necessary, so clean up
            latestBuildZip.delete()
        }
        createResponse(context) { it["message"] = "Deployment downloaded" }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SandraAPI::class.java)
    }

}
