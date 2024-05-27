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

package com.sandrabot.sandra.managers

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Service
import com.sandrabot.sandra.entities.Subscription
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Responsible for managing the subscription status of patrons and boosters.
 * The subscriptions are refreshed every 30 minutes while the service is running.
 */
class SubscriptionManager(private val sandra: Sandra) : Service(30.minutes, initialDelay = Duration.ZERO) {

    private val subscriptions = mutableMapOf<Long, MutableSet<Subscription>>()

    operator fun get(id: Long) = subscriptions[id] ?: emptySet()

    override suspend fun execute() {
        subscriptions.clear()

        // apply the developer subscription to all developers
        Constants.DEVELOPERS.forEach { putSet(it).add(Subscription.DEVELOPER) }

        // next apply the booster subscription to all boosters
        val hangout = sandra.shards.getGuildById(Constants.GUILD_HANGOUT)
        hangout?.boosters?.forEach { putSet(it.idLong).add(Subscription.BOOSTER) }

    }

    private fun putSet(id: Long) = subscriptions.getOrPut(id) { mutableSetOf() }

}
