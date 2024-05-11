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
import com.sandrabot.sandra.utils.canReputation
import com.sandrabot.sandra.utils.format
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
class Reputation : Command(arguments = "[@user]") {

    override suspend fun execute(event: CommandEvent) {

        // only allow users to give reputation points once every 20 hours
        if (!event.userConfig.canReputation() && !event.isOwner) {
            val nextRep = event.userConfig.reputationLast + 72_000_000 // 20 hours
            val remaining = ((nextRep - System.currentTimeMillis()) / 1_000).seconds.format()
            event.replyEmote(event.get("cooldown", remaining), Emotes.TIME).setEphemeral(true).queue()
            return
        }

        // user is guaranteed to be non-null since it's required
        val targetUser = event.arguments.user()!!
        // prevent bots from receiving rep and creating data profiles
        if (targetUser.isBot || targetUser.isSystem) {
            event.replyError(event.get("no_bots")).setEphemeral(true).queue()
            return
        }

        // don't allow the user to give reputation to themselves either
        if (targetUser == event.user) {
            event.replyError(event.get("no_self")).setEphemeral(true).queue()
            return
        }

        // increment the reputation of the target user
        val targetConfig = event.sandra.config[targetUser].apply { reputation++ }
        // update the current user's reputation timer
        event.userConfig.reputationLast = System.currentTimeMillis()

        // reply with the target user's updated rep count
        val reply = event.get("reply", targetUser, targetConfig.reputation.format())
        event.replyEmote(reply, Emotes.ADD).queue()

    }

}
