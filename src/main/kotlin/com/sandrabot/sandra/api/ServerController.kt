/*
 * Copyright 2017-2023 Avery Carroll and Logan Devecka
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
import com.sandrabot.sandra.api.plugins.configureRouting
import com.sandrabot.sandra.api.plugins.configureSerialization
import com.sandrabot.sandra.api.plugins.configureStatusPages
import com.sandrabot.sandra.utils.toJsonObject
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*

class ServerController(private val sandra: Sandra) {

    private val ktorServer = embeddedServer(Netty, port = sandra.settings.apiPort) { module() }

    private fun Application.module() {
        configureRouting(sandra)
        configureSerialization()
        configureStatusPages()
    }

    fun start() {
        ktorServer.start()
    }

    fun shutdown() {
        ktorServer.stop()
    }

}

suspend inline fun ApplicationCall.respondJson(
    status: HttpStatusCode = HttpStatusCode.OK, success: Boolean = true, data: Map<String, Any?> = emptyMap()
) {
    val dataMap = mapOf("code" to status.value, "success" to success, "data" to data)
    respond(status = status, dataMap.toJsonObject())
}
