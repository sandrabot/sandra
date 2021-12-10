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

package com.sandrabot.sandra.commands.essential

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.await
import com.sandrabot.sandra.utils.toFormattedString
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@Suppress("unused")
class Ping : Command(name = "ping") {

    @OptIn(ExperimentalTime::class)
    override suspend fun execute(event: CommandEvent) {

        val restPing = event.jda.restPing.await()
        val rest = restPing.toDuration(DurationUnit.MILLISECONDS).toFormattedString()
        val formattedRest = if (restPing > 250) "${Emotes.WARN} $rest" else rest

        val websocketAverage = event.jda.gatewayPing
        val websocket = websocketAverage.toDuration(DurationUnit.MILLISECONDS).toFormattedString()
        val formattedWebsocket = if (websocketAverage > 250) "${Emotes.WARN} $websocket" else websocket

        event.replyInfo(event.translate("reply", formattedRest, formattedWebsocket))

    }

}
