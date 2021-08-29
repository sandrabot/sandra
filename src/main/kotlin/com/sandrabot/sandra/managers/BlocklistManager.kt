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

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.RedisPrefix
import com.sandrabot.sandra.entities.blocklist.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Keeps track of blocked features and offence history.
 */
@OptIn(ExperimentalSerializationApi::class)
class BlocklistManager(private val sandra: Sandra) {

    private val entries = mutableMapOf<Long, BlocklistEntry>()

    init {
        val data = sandra.redis[RedisPrefix.SETTING + "blocklist"] ?: "[]"
        Json.decodeFromString<List<BlocklistEntry>>(data).forEach { entries[it.targetId] = it }
    }

    fun getEntry(targetId: Long): BlocklistEntry? = entries[targetId]

    fun shutdown() {
        sandra.redis[RedisPrefix.SETTING + "blocklist"] = Json.encodeToString(entries.values.toList())
    }

    fun appendOffence(
        targetId: Long, targetType: TargetType, features: List<FeatureType>,
        expiresAt: Long, moderator: Long, automated: Boolean, reason: String
    ) {
        val entry = if (targetId in entries) entries[targetId] else {
            BlocklistEntry(targetId, targetType, mutableListOf(), mutableListOf()).also {
                entries[targetId] = it
            }
        }
        val blockedFeatures = entry!!.blockedFeatures
        synchronized(blockedFeatures) {
            // By removing features that are already blocked, we guarantee
            // only the most recent offence expresses the expiration timestamp
            blockedFeatures.removeIf { it.feature in features }
            val additionalFeatures = features.map { BlockedFeature(it, expiresAt) }
            blockedFeatures.addAll(additionalFeatures)
        }
        val offences = entry.offences
        synchronized(offences) {
            val timestamp = System.currentTimeMillis() / 1000
            val currentOffence = BlocklistOffence(features, moderator, timestamp, automated, reason)
            offences.add(currentOffence)
        }
    }

}
