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
import com.sandrabot.sandra.constants.Constants.PATREON_CAMPAIGN
import com.sandrabot.sandra.entities.Service
import com.sandrabot.sandra.entities.Subscription
import com.sandrabot.sandra.utils.array
import com.sandrabot.sandra.utils.httpClient
import com.sandrabot.sandra.utils.obj
import com.sandrabot.sandra.utils.string
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Responsible for managing the subscription status of patrons and boosters.
 * The subscriptions are refreshed every 30 minutes while the service is running.
 */
class SubscriptionManager(private val sandra: Sandra) : Service(1800, initialDelay = 0) {

    private val subscriptions = mutableMapOf<Long, MutableSet<Subscription>>()

    operator fun get(id: Long) = subscriptions[id] ?: emptySet()

    override suspend fun execute() {
        subscriptions.clear()

        // apply the developer subscription to all developers
        Constants.DEVELOPERS.forEach { putSet(it).add(Subscription.DEVELOPER) }

        // next apply the booster subscription to all boosters
        val hangout = sandra.shards.getGuildById(Constants.GUILD_HANGOUT)
        hangout?.boosters?.forEach { putSet(it.idLong).add(Subscription.BOOSTER) }

        var currentRoute = PATREON_ROUTE
        val dataPages = mutableListOf<JsonObject>()
        // use a loop to read the entire paginated response
        do dataPages += httpClient.get(currentRoute) {
            header("Authorization", "Bearer ${sandra.settings.secrets.patreonToken}")
        }.body<JsonObject>().also { currentRoute = it.obj("links")?.string("next") ?: "" }
        while (currentRoute.isNotEmpty())

        // parse the responses and apply subscriptions to patrons
        val members = dataPages.flatMap { it.array("data")?.map { m -> m.jsonObject } ?: emptyList() }
        val users = dataPages.flatMap { it.array("included")?.map { u -> u.jsonObject } ?: emptyList() }
            .filter { it.string("type") == "user" }

        for (member in members.mapNotNull { it.obj("relationships") }) {
            val id = member.obj("user")?.obj("data")?.string("id")
            val entitled = member.obj("currently_entitled_tiers")?.array("data")
                ?.mapNotNull { fromTierId(it.jsonObject.string("id")) }?.toMutableSet()
            if (entitled.isNullOrEmpty()) continue // so apparently you can be a patron but not have a subscription
            val social = users.firstOrNull { it.string("id") == id }?.obj("attributes")?.obj("social_connections")
            val discordId = social?.obj("discord")?.string("user_id")?.toLongOrNull() ?: continue
            // todo maybe notify the user if they have a subscription but it's not linked to their discord?
            putSet(discordId).addAll(entitled)
        }

    }

    private fun putSet(id: Long) = subscriptions.getOrPut(id) { mutableSetOf() }

    private fun fromTierId(id: String?) = when (id?.toIntOrNull()) {
        Constants.PATREON_DONOR -> Subscription.DONOR
        Constants.PATREON_SPONSOR -> Subscription.SPONSOR
        Constants.PATREON_BENEFACTOR -> Subscription.BENEFACTOR
        else -> null
    }

    private companion object {
        private const val PATREON_ROUTE =
            "https://www.patreon.com/api/oauth2/v2/campaigns/$PATREON_CAMPAIGN/members?include=currently_entitled_tiers,user&fields%5Buser%5D=social_connections"
    }

}
