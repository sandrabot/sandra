/*
 *    Copyright 2017-2020 Avery Clifton and Logan Devecka
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sandrabot.sandra.services

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Service
import com.sandrabot.sandra.utils.HttpUtil
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.internal.JDAImpl
import org.json.JSONObject

/**
 * Updates the bot's listings on various Discord Bot Lists.
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
        val onDiscordData = JSONObject().put("guildCount", guilds.sum())
        send(onDiscordUrl, sandra.credentials.botsOnDiscordToken, onDiscordData)

        // https://top.gg/bot/302915036492333067
        val topData = JSONObject().put("shards", guilds)
        send(topGgUrl, sandra.credentials.topGgToken, topData)

        for (i in guilds.indices) {

            // https://discord.bots.gg/bots/302915036492333067
            val botsGgData = JSONObject().put("guildCount", guilds[i])
            botsGgData.put("shardCount", guilds.size).put("shardId", i)
            send(discordBotsUrl, sandra.credentials.discordBotsGgToken, botsGgData)

            // https://discordbotlist.com/bots/302915036492333067
            val dblData = JSONObject().put("voice_connections", voice[i])
            dblData.put("shard_id", i).put("guilds", guilds[i])
            send(dblUrl, sandra.credentials.discordBotListToken, dblData)

        }

    }

    private fun send(url: String, token: String, data: JSONObject) {
        val route = url.replace("{}", Constants.APPLICATION_ID.toString())
        val body = HttpUtil.Companion.createBody(HttpUtil.APPLICATION_JSON, data.toString())
        val request = HttpUtil.createRequest(route, "POST", body)
        request.header("Authorization", token)
        HttpUtil.execute(request.build())
    }

    companion object {
        private const val onDiscordUrl = "https://bots.ondiscord.xyz/bot-api/bots/{}/guilds"
        private const val topGgUrl = "https://top.gg/api/bots/{}/stats"
        private const val discordBotsUrl = "https://discord.bots.gg/api/v1/bots/{}/stats"
        private const val dblUrl = "https://discordbotlist.com/api/bots/{}/stats"
    }

}
