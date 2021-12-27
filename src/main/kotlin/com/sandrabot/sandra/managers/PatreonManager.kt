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

package com.sandrabot.sandra.managers

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.PatreonTier
import com.sandrabot.sandra.entities.Service
import com.sandrabot.sandra.utils.array
import com.sandrabot.sandra.utils.getBlocking
import com.sandrabot.sandra.utils.obj
import com.sandrabot.sandra.utils.string
import io.ktor.client.request.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory

/**
 * Maintains a map of patrons and the tier they are subscribed to.
 * The map is refreshed every 30 minutes while the service is running.
 */
class PatreonManager(private val sandra: Sandra) : Service(1800) {

    private val userTiers = mutableMapOf<Long, PatreonTier>()

    fun getUserTier(id: Long) = if (id in Constants.DEVELOPERS) PatreonTier.SPONSOR else userTiers[id]

    init {
        start(initialDelay = 0)
    }

    override fun execute() {
        // This is a map of patreon user IDs to the tier ID they are subscribed to
        val patreonUserToTier = mutableMapOf<String, String>()
        // Map of patreon user IDs to their discord user IDs
        val patreonUserToDiscordUser = mutableMapOf<String, String>()
        // Figures out which reward ID is which tier
        val patreonRewardToTier = mutableMapOf<String, PatreonTier>()

        userTiers.clear()
        var nextUrl: String? = apiUrl
        do {
            val apiResponse = getBlocking<String>(nextUrl!!) {
                header("Authorization", "Bearer ${sandra.credentials.patreonToken}")
            }
            val json = Json.parseToJsonElement(apiResponse) as JsonObject
            // If there were any errors in the response, log it and stop here
            if ("errors" in json) {
                logger.error("Failed to update patreon campaign pledges: {}", apiResponse)
                return
            }

            // Figure out which patreon users have which rewards
            json.array("data")?.map { it.jsonObject }?.filter {
                it.string("type") == "pledge" && it.obj("attributes")?.string("declined_since") == null
            }?.map { it.obj("relationships")!! }?.forEach {
                val userId = it.obj("patron")?.obj("data")?.string("id")!!
                patreonUserToTier[userId] = it.obj("reward")?.obj("data")?.string("id")!!
            }

            // Figure out which users have connected their discord accounts
            val included = json.array("included")?.map { it.jsonObject }!!
            included.filter { it.string("type") == "user" && it.string("id") in patreonUserToTier }.forEach {
                it.obj("attributes")?.obj("social_connections")?.obj("discord")?.string("user_id")?.let { userId ->
                    patreonUserToDiscordUser[it.string("id")!!] = userId
                }
            }

            // Figure out which reward id is which reward tier
            included.filter {
                it.string("type") == "reward" && it.string("id")!!.toInt() > 0 &&
                        it.obj("relationships")?.obj("campaign")?.obj("data")?.string("id")
                            ?.toInt()!! == Constants.PATREON_CAMPAIGN
            }.forEach {
                val rewardTitle = it.obj("attributes")?.string("title")!!.filter { ch -> ch.isLetter() }
                patreonRewardToTier[it.string("id")!!] = PatreonTier.valueOf(rewardTitle.uppercase())
            }

            // The api responses are paginated for whatever reason
            nextUrl = json.obj("links")?.string("next")
        } while (nextUrl != null)

        // Map the discord IDs to their respective tiers
        for (pledge in patreonUserToTier) {
            // If the discord ID is null, the patron hasn't connected their discord account yet
            val discordId = patreonUserToDiscordUser[pledge.key]?.toLongOrNull() ?: continue
            userTiers[discordId] = patreonRewardToTier[pledge.value]!!
        }

        // Anyone who boosts the support server will also get tier 1 rewards
        sandra.shards.getGuildById(Constants.GUILD_HANGOUT)?.boosters?.forEach {
            if (userTiers[it.idLong] == null) userTiers[it.idLong] = PatreonTier.TIPPER
        }
    }

    companion object {
        private const val apiUrl = "https://patreon.com/api/oauth2/api/campaigns/${Constants.PATREON_CAMPAIGN}/pledges"
        private val logger = LoggerFactory.getLogger(PatreonManager::class.java)
    }

}
