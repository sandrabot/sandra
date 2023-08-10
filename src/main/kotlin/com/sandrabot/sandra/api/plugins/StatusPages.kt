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

package com.sandrabot.sandra.api.plugins

import com.sandrabot.sandra.api.exceptions.CallResponseException
import com.sandrabot.sandra.api.respondJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        val failures = HttpStatusCode.allStatusCodes.filter { it.value in 400 until 600 }.toTypedArray()
        status(*failures) { call, status ->
            call.respondJson(status, success = false, mapOf("message" to status.description))
        }
        exception<CallResponseException> { call, cause ->
            call.respondJson(cause.status, success = false, mapOf("message" to cause.message))
        }
    }
}
