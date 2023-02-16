/*
 * Copyright 2017-2023 Avery Carroll and Logan Devecka
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
import dev.minn.jda.ktx.coroutines.await

@Suppress("unused")
class Reputation : Command(arguments = "[@user]") {
    override suspend fun execute(event: CommandEvent) {
        val otherUser = event.arguments.user()!!
        // don't allow users to give reputation to themselves
        if (event.user == otherUser) {
            event.replyError(event.get("self")).setEphemeral(true).await()
            return
        }
        // only allow users to give reputation once every 20 hours
        if (event.userConfig.canReputation() || event.isOwner) {
            // increment the reputation of the other user
            val config = event.sandra.config[otherUser].apply { reputation++ }
            // update the last reputation time
            event.userConfig.reputationLast = System.currentTimeMillis()
            // reply with the new reputation count
            event.replyEmote(event.get("reply", otherUser, config.reputation.format()), Emotes.ADD).await()
        } else event.replyError(event.get("cooldown")).setEphemeral(true).await() // reply with the cooldown error
    }
}
