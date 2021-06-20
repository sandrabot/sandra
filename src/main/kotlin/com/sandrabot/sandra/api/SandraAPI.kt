/*
 * Copyright 2017-2021 Avery Carroll and Logan Devecka
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

import com.beust.klaxon.JsonObject
import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.Context
import org.slf4j.LoggerFactory

/**
 * This class receives and processes calls to the Sandra API.
 */
class SandraAPI(private val sandra: Sandra, private val port: Int) {

    private val api: Javalin = Javalin.create { config ->
        config.defaultContentType = "application/json"
        config.logIfServerNotStarted = false
        config.showJavalinBanner = false
        if (sandra.development) config.enableDevLogging()
    }

    init {

        api.before {
            logger.info("Received ${it.method()} ${it.url()} from ${it.ip()} ${it.userAgent()}")
            sandra.statistics.incrementRequestCount()
        }

        api.routes {
            path("/api/v1") {
                // A path is used here because more
                // routes will be added in the future
                get("/status", ::status)
            }
        }

        api.error(404) {
            createError(it, it.status(), "Resource not found")
        }

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

    private fun createResponse(context: Context, handler: ((JsonObject) -> Unit)? = null) {
        val response = JsonObject()
        response["success"] = true
        if (handler != null) handler(response)
        response["version"] = Constants.VERSION
        // Set the response body to the JSON with 2 spaces as indentation
        context.result(response.toJsonString(true))
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

    companion object {
        private val logger = LoggerFactory.getLogger(SandraAPI::class.java)
    }

}
