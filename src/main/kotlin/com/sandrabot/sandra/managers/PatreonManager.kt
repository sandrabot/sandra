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

package com.sandrabot.sandra.managers

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.lookup
import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.PatreonTier
import com.sandrabot.sandra.utils.getBlocking
import io.ktor.client.request.*
import org.slf4j.LoggerFactory
import java.io.StringReader

/**
 * Keeps a cache of patrons and the rewards they currently have.
 * If a request is made and the cache is older
 * than 5 minutes, the cache will be refreshed.
 */
class PatreonManager(private val sandra: Sandra) {

    private val pledges = mutableMapOf<Long, PatreonTier>()
    private var lastUpdate: Long = 0

    // This is only intended to be called via an eval
    @Suppress("unused")
    fun updatePledgesNow() {
        lastUpdate = 0
        updatePledges()
    }

    @Synchronized
    private fun updatePledges() {
        // Check if we need to update the pledges, synchronously
        val minutesSinceUpdate = (System.currentTimeMillis() - lastUpdate) / 60000
        // Only update them if our cache is older than 30 minutes
        if (minutesSinceUpdate < 30) return
        // Patreon User ID to Reward ID
        val userToPledge = mutableMapOf<String, String>()
        // Patreon User ID to Discord User ID
        val userToDiscord = mutableMapOf<String, String>()
        // Reward ID to Tier
        val rewardToTier = mutableMapOf<String, PatreonTier>()
        var nextUrl: String? = pledgesUrl
        do {
            val response = getBlocking<String>(nextUrl!!) {
                header("Authorization", "Bearer ${sandra.credentials.patreonToken}")
            }
            val json = Klaxon().parseJsonObject(StringReader(response))
            // If there was an authentication error log it and stop here
            if ("errors" in json) {
                logger.warn("Failed to retrieve campaign pledges: {}", json.toJsonString())
                break
            }
            // Figure out which patreon users have which rewards
            val data = json.array<JsonObject>("data")!!
            data.filter {
                it["type"] == "pledge" && it.obj("attributes")?.string("declined_since") == null
            }.map {
                it.lookup<String>("relationships.patron.data.id")[0] to it.lookup<String>("relationships.reward.data.id")[0]
            }.forEach { userToPledge[it.first] = it.second }
            // Figure out which users have connected their discord accounts
            val included = json.array<JsonObject>("included")!!
            included.filter {
                it["type"] == "user" && it["id"] in userToPledge
            }.forEach {
                userToDiscord[it.string("id")!!] = it.lookup<String>("attributes.social_connections.discord.user_id")[0]
            }
            // Figure out which reward id is which reward tier
            included.filter {
                it["type"] == "reward" && it.string("id")!!.toInt() > 0
            }.forEach {
                val rewardName = it.obj("attributes")!!.string("title")!!.filter { ch -> ch.isLetter() }
                rewardToTier[it.string("id")!!] = PatreonTier.valueOf(rewardName.toUpperCase())
            }
            nextUrl = json.obj("links")!!.string("next")
        } while (nextUrl != null)
        pledges.clear()
        for (pledge in userToPledge) {
            // If this is null, the patron didn't connect their discord account yet
            val discordId = userToDiscord[pledge.key]?.toLongOrNull() ?: continue
            pledges[discordId] = rewardToTier[pledge.value]!!
        }
        // Our boosters should also get a treat for helping us out
        sandra.shards.getGuildById(Constants.GUILD_HANGOUT)?.let { guild ->
            guild.boostRole?.let { boostRole ->
                guild.getMembersWithRoles(boostRole).forEach {
                    if (pledges[it.idLong] != PatreonTier.SPONSOR) pledges[it.idLong] = PatreonTier.DONOR
                }
            }
        }
        lastUpdate = System.currentTimeMillis()
    }

    /**
     * Returns the reward tier for the user, or null if they are not a patron.
     */
    fun getUserTier(userId: Long): PatreonTier? {
        updatePledges()
        return pledges[userId]
    }

    companion object {
        private const val pledgesUrl =
            "https://patreon.com/api/oauth2/api/campaigns/${Constants.PATREON_CAMPAIGN}/pledges"
        private val logger = LoggerFactory.getLogger(PatreonManager::class.java)
    }

}
