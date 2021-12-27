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

import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.entities.Paginator
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.capitalizeWords
import com.sandrabot.sandra.utils.format
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed

@Suppress("unused")
class Shards : Command(name = "shards") {

    override suspend fun execute(event: CommandEvent) {

        event.deferReply(ephemeral = true).queue()
        val shardFields = event.sandra.shards.shardCache.sortedBy { it.shardInfo.shardId }.map {
            val status = it.status.name.replace("_", " ").capitalizeWords()
            val value = event.translate("status", status, it.gatewayPing.format(), it.guildCache.size().format())
            MessageEmbed.Field(event.translate("shard_title", it.shardInfo.shardId), value, true)
        }
        val size = event.sandra.shards.shardCache.size()
        val connected = event.sandra.shards.shardCache.count { it.status == JDA.Status.CONNECTED }
        val embed = event.embed.setTitle(event.translate("having_issues"), Constants.DIRECT_SUPPORT)
        embed.setFooter(event.translate("shards_connected", connected, size) + if (event.isFromGuild)
            event.translate("server_using", event.jda.shardInfo.shardId) else ""
        )
        Paginator(event).paginate(shardFields.chunked(9).map { chunk ->
            embed.clearFields().also { it.fields.addAll(chunk) }.build()
        })

    }

}
