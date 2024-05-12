/*
 * Copyright 2017-2024 Avery Carroll and Logan Devecka
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

import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral

@Suppress("unused")
class Magic8Ball : Command(arguments = "[@question:text]") {

    // normally the command name is inferred by the class name, however this
    // is a special case, since class names should start with uppercase letters
    // this is why the class name is Magic8Ball instead of just 8Ball or EightBall
    override val name = "8ball"

    override suspend fun execute(event: CommandEvent) {

        // it's guaranteed that question will never be null, since it's required
        val question = event.arguments.text(name = "question")!!
        // only allow users to submit shorter questions to prevent spamming
        if (question.length > 60) {
            event.replyError(event.get("too_long")).asEphemeral().queue()
            return
        }

        // since the translation path for the answer is an array, one will already be picked at random
        val response = event.get("reply", Unicode.MAGIC_8_BALL, event.user, question, event.get("answer"))
        // disable mentions to prevent users from spamming each other with the bot
        event.reply(response).setAllowedMentions(emptySet()).queue()

    }

}
