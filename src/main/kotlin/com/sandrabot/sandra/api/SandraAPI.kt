/*
 *    Copyright 2017-2020 Avery Clifton and Logan Devecka
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sandrabot.sandra.api

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.Context
import org.json.JSONObject
import org.slf4j.LoggerFactory

/**
 * This class receives and processes calls to the Sandra API.
 */
class SandraAPI(private val sandra: Sandra, private val port: Int) {

    private val api = Javalin.create { config ->
        config.defaultContentType = "application/json"
        config.logIfServerNotStarted = false
        config.showJavalinBanner = false
        if (sandra.developmentMode) config.enableDevLogging()
    }.routes {
        path("/api/v1") {
            get(::welcome)
            get("/status", ::status)
        }
    }.before {
        logger.info("Received ${it.method()} ${it.url()} from ${it.ip()} ${it.userAgent()}")
        sandra.statistics.incrementRequestCount()
    }

    fun start() {
        api.start(port)
    }

    fun shutdown() {
        api.stop()
    }

    /* Route Handlers */

    private fun welcome(context: Context) {
        val response = JSONObject()
        response.put("message", "Welcome to the Sandra API!")
        response.put("version", Constants.VERSION)
        context.result(response.toString())
    }

    private fun status(context: Context) {
        val response = JSONObject()
        response.put("ping", sandra.shards.averageGatewayPing)
        response.put("guilds", sandra.shards.guildCache.size())
        response.put("requests", sandra.statistics.requestCount)
        val runtime = Runtime.getRuntime()
        response.put("memory", (runtime.totalMemory() - runtime.freeMemory()) shr 20)
        response.put("uptime", (System.currentTimeMillis() - sandra.statistics.startTime) / 1000)
        response.put("version", Constants.VERSION)
        context.result(response.toString())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SandraAPI::class.java)
    }

}
