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

package com.sandrabot.sandra.services

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Service
import com.sandrabot.sandra.utils.HTTP_CLIENT
import io.ktor.client.request.*
import net.dv8tion.jda.api.JDA

/**
 * Periodically posts server count data to various bot lists.
 */
class BotListService(private val sandra: Sandra) : Service(300) {

    override suspend fun execute() {

        // ensure we have an accurate server count
        if (sandra.shards.shardCache.any { it.status != JDA.Status.CONNECTED }) return
        val guilds = sandra.shards.shardCache.map { it.guildCache.size() }

        // https://bots.ondiscord.xyz/bots/302915036492333067
        send(onDiscordUrl, sandra.settings.secrets.bodToken, mapOf("guildCount" to guilds.sum()))

        // https://discord.boats/bot/302915036492333067
        send(boatUrl, sandra.settings.secrets.boatToken, mapOf("server_count" to guilds.sum()))

        // https://top.gg/bot/302915036492333067
        send(topGgUrl, sandra.settings.secrets.topGgToken, mapOf("shards" to guilds))

        // https://botlist.space/bot/302915036492333067
        send(spaceUrl, sandra.settings.secrets.spaceToken, mapOf("shards" to guilds))

        guilds.forEachIndexed { i, count ->
            // https://discord.bots.gg/bots/302915036492333067
            val botsGgData = mapOf("guildCount" to count, "shardCount" to guilds.size, "shardId" to i)
            send(discordBotsUrl, sandra.settings.secrets.dbgToken, botsGgData)

            // https://discordbotlist.com/bots/302915036492333067
            send(dblUrl, sandra.settings.secrets.dblToken, mapOf("shard_id" to i, "guilds" to count))
        }

    }

    private suspend fun send(url: String, token: String, data: Map<String, Any>) =
        HTTP_CLIENT.post(url.format(Constants.APPLICATION_ID)) {
            header("Authorization", token)
            setBody(data)
        }

    private companion object {
        private const val onDiscordUrl = "https://bots.ondiscord.xyz/bot-api/bots/%d/guilds"
        private const val boatUrl = "https://discord.boats/api/bot/%d"
        private const val topGgUrl = "https://top.gg/api/bots/%d/stats"
        private const val spaceUrl = "https://api.botlist.space/v1/bots/%d"
        private const val discordBotsUrl = "https://discord.bots.gg/api/v1/bots/%d/stats"
        private const val dblUrl = "https://discordbotlist.com/api/bots/%d/stats"
    }

}
