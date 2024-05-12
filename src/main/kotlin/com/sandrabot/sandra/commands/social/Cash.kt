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

package com.sandrabot.sandra.commands.social

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.format

@Suppress("unused")
class Cash : Command(arguments = "[user]") {

    override suspend fun execute(event: CommandEvent) {

        // allow the user to view someone else's balance
        val targetUser = event.arguments.user() ?: event.user
        // retrieve the balance of the target user and format it
        val cash = event.sandra.config[targetUser].cash.format()
        val reply = event.get(if (targetUser == event.user) "self" else "other", targetUser, cash)
        // to prevent mention spam, disable all mentions in the reply
        event.replyEmote(reply, Emotes.CASH).setAllowedMentions(emptySet()).queue()

    }

}
