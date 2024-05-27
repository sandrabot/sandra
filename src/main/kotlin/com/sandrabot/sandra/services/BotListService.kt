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

package com.sandrabot.sandra.services

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Service
import com.sandrabot.sandra.utils.HTTP_CLIENT
import io.ktor.client.request.*
import net.dv8tion.jda.api.JDA
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Periodically posts guild count data to various bot listing sites.
 */
class BotListService(private val sandra: Sandra) : Service(6.hours, initialDelay = 5.minutes) {

    override suspend fun execute() {

        // ensure we have an accurate server count
        if (sandra.shards.shardCache.any { it.status != JDA.Status.CONNECTED }) return
        val guildsPerShard = sandra.shards.shardCache.map { it.guildCache.size() }

        // https://top.gg/bot/302915036492333067
        send(TOP_GG, sandra.settings.secrets.topGgToken, mapOf("shards" to guildsPerShard))

        // https://botlist.space/bot/302915036492333067
        send(BOTLIST_SPACE, sandra.settings.secrets.spaceToken, mapOf("shards" to guildsPerShard))

        guildsPerShard.forEachIndexed { shard, guilds ->
            // https://discord.bots.gg/bots/302915036492333067
            val botsGgData = mapOf("guildCount" to guilds, "shardCount" to guildsPerShard.size, "shardId" to shard)
            send(BOTS_GG, sandra.settings.secrets.dbgToken, botsGgData)

            // https://discordbotlist.com/bots/302915036492333067
            send(DISCORD_BOT_LIST, sandra.settings.secrets.dblToken, mapOf("shard_id" to shard, "guilds" to guilds))
        }

    }

    private suspend fun send(
        url: String, token: String, data: Map<String, Any>,
    ) = HTTP_CLIENT.post(url.format(Constants.APPLICATION_ID)) {
        header("Authorization", token)
        setBody(data)
    }

    private companion object {
        private const val TOP_GG = "https://top.gg/api/bots/%d/stats"
        private const val BOTLIST_SPACE = "https://api.botlist.space/v1/bots/%d"
        private const val BOTS_GG = "https://discord.bots.gg/api/v1/bots/%d/stats"
        private const val DISCORD_BOT_LIST = "https://discordbotlist.com/api/bots/%d/stats"
    }

}
