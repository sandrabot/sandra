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

package com.sandrabot.sandra.api.handlers

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.api.jsonResult
import com.sandrabot.sandra.entities.EndpointHandler
import io.javalin.http.Context
import io.javalin.http.HandlerType

@Suppress("unused")
class StatusHandler(private val sandra: Sandra) : EndpointHandler("status", HandlerType.GET) {

    private val runtime by lazy { Runtime.getRuntime() }

    override fun handle(context: Context): Unit = arrayOf(
        "ping" to sandra.shards.averageGatewayPing,
        "guilds" to sandra.shards.guildCache.size(),
        "memory" to (runtime.totalMemory() - runtime.freeMemory() shr 20),
        "uptime" to (System.currentTimeMillis() - sandra.statistics.startTime) / 1000
    ).let { jsonResult(context, elements = it) }

}
