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

package com.sandrabot.sandra.commands.social

import com.sandrabot.sandra.constants.Emojis
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.utils.format

@Suppress("unused")
class Cash : Command(arguments = "[user]") {

    override suspend fun execute(event: CommandEvent) {

        // unless the user argument was supplied, display the current user's balance
        val targetUser = event.arguments.user() ?: event.user
        // prevent bots from creating data profiles
        if (targetUser.isBot || targetUser.isSystem) {
            event.replyNotice(event.get("no_bots")).asEphemeral().queue()
            return
        }
        // retrieve the balance of the target user and format it
        val cash = event.sandra.config[targetUser].cash.format()
        // figure out which response to show to the user
        val path = if (targetUser == event.user) "self" else "other"
        val reply = event.get(path, targetUser, cash)
        // to prevent mentioning other users, disable all mentions in the response
        event.replyEmoji(Emojis.CASH, reply).setAllowedMentions(emptySet()).queue()

    }

}
