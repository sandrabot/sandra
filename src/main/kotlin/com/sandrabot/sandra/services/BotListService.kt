/*
 * Copyright 2017-2020 Avery Clifton and Logan Devecka
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

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Service
import com.sandrabot.sandra.utils.postBlocking
import io.ktor.client.request.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.internal.JDAImpl

/**
 * Updates the bot listings on various discord bot list sites.
 */
class BotListService(private val sandra: Sandra) : Service(300) {

    override fun execute() {

        // Ensure we never send any partial data to the bot lists
        if (sandra.shards.shardCache.any { it.status != JDA.Status.CONNECTED }) return

        val guilds = ArrayList<Long>()
        val voice = ArrayList<Int>()

        // Gather the stats from the shards
        sandra.shards.shardCache.forEach { shard ->
            guilds.add(shard.guildCache.size())
            voice.add((shard as JDAImpl).audioManagerCache.count { it.isConnected })
        }

        // https://bots.ondiscord.xyz/bots/302915036492333067
        val onDiscordData = json { obj("guildCount" to guilds.sum()) }
        send(onDiscordUrl, sandra.credentials.bodToken, onDiscordData)

        // https://discord.boats/bot/302915036492333067
        val boatData = json { obj("server_count" to guilds.sum()) }
        send(boatUrl, sandra.credentials.boatToken, boatData)

        // https://top.gg/bot/302915036492333067
        val topData = json { obj("shards" to guilds) }
        send(topGgUrl, sandra.credentials.topGgToken, topData)

        // https://botlist.space/bot/302915036492333067
        // We can reuse the JSON here because this API follows the same format
        send(spaceUrl, sandra.credentials.spaceToken, topData)

        for (i in guilds.indices) {

            // https://discord.bots.gg/bots/302915036492333067
            val botsGgData = json { obj("guildCount" to guilds[i], "shardCount" to guilds.size, "shardId" to i) }
            send(discordBotsUrl, sandra.credentials.dbgToken, botsGgData)

            // https://discordbotlist.com/bots/302915036492333067
            val dblData = json { obj("voice_connections" to voice[i], "shard_id" to i, "guilds" to guilds[i]) }
            send(dblUrl, sandra.credentials.dblToken, dblData)

        }

    }

    private fun send(url: String, token: String, data: JsonObject) {
        val route = url.replace("{}", Constants.APPLICATION_ID.toString())
        postBlocking<Unit>(route, data.toJsonString()) { header("Authorization", token) }
    }

    companion object {
        private const val onDiscordUrl = "https://bots.ondiscord.xyz/bot-api/bots/{}/guilds"
        private const val boatUrl = "https://discord.boats/api/bot/{}"
        private const val topGgUrl = "https://top.gg/api/bots/{}/stats"
        private const val spaceUrl = "https://api.botlist.space/v1/bots/{}"
        private const val discordBotsUrl = "https://discord.bots.gg/api/v1/bots/{}/stats"
        private const val dblUrl = "https://discordbotlist.com/api/bots/{}/stats"
    }

}
