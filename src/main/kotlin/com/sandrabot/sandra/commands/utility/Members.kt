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

package com.sandrabot.sandra.commands.utility

import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.format
import com.sandrabot.sandra.utils.sanitize

@Suppress("unused")
class Members : Command(name = "members", guildOnly = true) {

    override suspend fun execute(event: CommandEvent) {

        val memberCount = event.guild!!.memberCount
        val botCount = event.guild.memberCache.count { it.user.isBot }
        val humanCount = memberCount - botCount
        event.replyInfo(
            event.translate(
                "reply", event.guild.name.sanitize(), humanCount.format(), botCount.format(), memberCount.format()
            )
        ).allowedMentions(emptyList()).queue()

    }

}
