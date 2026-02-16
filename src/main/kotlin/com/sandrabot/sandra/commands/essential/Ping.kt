/*
 * Copyright 2017-2026 Avery Carroll and Logan Devecka
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

import com.sandrabot.sandra.constants.Emojis
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.format
import dev.minn.jda.ktx.coroutines.await
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused")
class Ping : Command() {

    override suspend fun execute(event: CommandEvent) {

        event.deferReply(ephemeral = true).await()
        val rest = event.jda.restPing.await().format()
        val gateway = event.jda.gatewayPing.format()
        event.sendInfo(event.get("reply", rest, gateway)).queue()

    }

    private fun Long.format(): String {
        val formatted = this.milliseconds.format()
        return if (this > 250) "${Emojis.NOTICE} $formatted" else formatted
    }

}
