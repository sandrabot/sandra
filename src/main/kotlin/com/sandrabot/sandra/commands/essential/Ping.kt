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
import com.sandrabot.sandra.utils.format
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused")
class Ping : Command(name = "ping") {

    override suspend fun execute(event: CommandEvent) {

        event.deferReply(ephemeral = true).await()
        val (rest, websocket) = arrayOf(event.jda.restPing.await(), event.jda.gatewayPing).map { ping ->
            val formatted = ping.milliseconds.format()
            if (ping > 250) "${Emotes.WARN} $formatted" else formatted
        }
        event.sendInfo(event.translate("reply", rest, websocket)).queue()

    }

}
