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

package com.sandrabot.sandra.commands.`fun`

import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.utils.useResourceStream
import dev.minn.jda.ktx.messages.MessageCreate

@Suppress("unused")
class Fortune : Command() {

    override suspend fun execute(event: CommandEvent) {

        // respond to the interaction with a random fortune
        event.reply(MessageCreate(useComponentsV2 = true) {
            container {
                text(event.get("reply", Unicode.FORTUNE_COOKIE))
                separator()
                text("> ${FORTUNES.random()}")
            }
        }).asEphemeral().queue()

    }

    private companion object {
        // load all the fortunes from a file, each line is an entry
        private val FORTUNES = useResourceStream("fortunes.txt") { String(readBytes()).lines() }
    }

}
