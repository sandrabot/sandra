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

package com.sandrabot.sandra.api.routes

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.api.respondJson
import io.ktor.server.routing.*

private val runtime by lazy { Runtime.getRuntime() }

fun Route.statusRouting(sandra: Sandra) {
    get("/status") {
        val data = mapOf(
            "ping" to sandra.shards.averageGatewayPing,
            "guilds" to sandra.shards.guildCache.size(),
            "memory" to (runtime.totalMemory() - runtime.freeMemory() shr 20),
            "uptime" to (System.currentTimeMillis() - sandra.statistics.startTime) / 1000,
            "shards" to sandra.shards.shardCache.map { jda ->
                mapOf(
                    "id" to jda.shardInfo.shardId,
                    "ping" to jda.gatewayPing,
                    "guilds" to jda.guildCache.size(),
                    "status" to jda.status.toString()
                )
            })
        call.respondJson(data = data)
    }
}
