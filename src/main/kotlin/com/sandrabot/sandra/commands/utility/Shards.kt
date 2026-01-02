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

package com.sandrabot.sandra.commands.utility

import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.format
import net.dv8tion.jda.api.JDA

@Suppress("unused")
class Shards : Command() {

    override suspend fun execute(event: CommandEvent) {

        val size = event.sandra.shards.shardCache.size()
        val connected = event.sandra.shards.shardCache.count { it.status == JDA.Status.CONNECTED }
        val embed = event.embed().setTitle(event.get("having_issues"), Constants.DIRECT_SUPPORT)
        val serverUsing = if (event.isFromGuild) event.get("server_using", event.jda.shardInfo.shardId) else ""
        embed.setFooter(event.get("shards_connected", connected, size) + serverUsing)
        event.sandra.shards.shardCache.sortedBy { it.shardInfo.shardId }.forEach {
            val status = it.status.name.replace("_", " ").lowercase()
            val value = event.get("status", status, it.gatewayPing.format(), it.guildCache.size().format())
            embed.addField(event.get("shard_title", it.shardInfo.shardId), value, true)
        }
        event.sendMessageEmbeds(embed.build()).queue()

    }

}
