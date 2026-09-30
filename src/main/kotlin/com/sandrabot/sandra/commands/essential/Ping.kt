/*
 * Copyright 2026 Avery Carroll, Logan Devecka, and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandrabot.sandra.commands.essential

import com.sandrabot.sandra.constants.Emojis
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.format
import dev.minn.jda.ktx.coroutines.await
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused")
class Ping : Command() {

    override suspend fun execute(event: CommandEvent) {

        // acknowledge the interaction before performing a slow rest action
        event.deferReply(ephemeral = true).queue()
        // request the current user to determine the general api response time
        val rest = event.jda.restPing.await().format()
        // a rough estimation of websocket latency from the last gateway heartbeat
        val gateway = event.jda.gatewayPing.format()
        event.sendInfo(event.get("reply", rest, gateway)).queue()

    }

    private fun Long.format(): String {
        val formatted = this.milliseconds.format()
        // prepend a warning emoji if the latency is above 250ms
        return if (this > 250) "${Emojis.NOTICE} $formatted" else formatted
    }

}
