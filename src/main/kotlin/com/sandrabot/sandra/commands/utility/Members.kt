/*
 * Copyright 2017-2022 Avery Carroll and Logan Devecka
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

package com.sandrabot.sandra.commands.utility

import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.format
import com.sandrabot.sandra.utils.sanitize
import dev.minn.jda.ktx.coroutines.await

@Suppress("unused")
class Members : Command(guildOnly = true) {

    override suspend fun execute(event: CommandEvent) {

        event.deferReply(ephemeral = true).await()
        val members = event.guild!!.memberCount
        // This is more accurate than checking the member cache
        val bots = event.guild.findMembers { it.user.isBot }.await().size
        val humans = (members - bots).format()
        val reply = event.get("reply", event.guild.name.sanitize(), humans, bots.format(), members.format())
        event.sendInfo(reply).setEphemeral(true).queue()

    }

}
