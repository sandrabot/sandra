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

package com.sandrabot.sandra.commands.`fun`

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import kotlin.random.Random

@Suppress("unused")
class Flip : Command(name = "flip") {

    override suspend fun execute(event: CommandEvent) {

        if (event.userConfig.credits <= 0) {
            event.replyEmote(event.translate("no_credits"), Emotes.CREDIT)
        } else {
            val (emote, side) = if (Random.nextBoolean()) Emotes.CREDIT to "heads" else Emotes.TAILS to "tails"
            event.replyEmote(event.translate("reply", event.localeContext.get("side_$side", true)), emote)
        }

    }

}
