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

package com.sandrabot.sandra.managers

import com.beust.klaxon.Klaxon
import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.RedisPrefix
import com.sandrabot.sandra.entities.blocklist.*
import gnu.trove.map.hash.TLongObjectHashMap

/**
 * Keeps track of blocked features and offence history.
 */
class BlocklistManager(private val sandra: Sandra) {

    private val entries = TLongObjectHashMap<BlocklistEntry>()

    init {
        val data = sandra.redis.get(RedisPrefix.SETTING + "blocklist") ?: "[]"
        Klaxon().parseArray<BlocklistEntry>(data)!!.forEach { entries.put(it.targetId, it) }
    }

    private fun saveEntries() {
        val data = Klaxon().toJsonString(entries.values())
        sandra.redis.set(RedisPrefix.SETTING + "blocklist", data)
    }

    fun appendOffence(targetId: Long, targetType: TargetType, features: List<FeatureType>,
                      expiresAt: Long, moderator: Long, automated: Boolean, reason: String) {
        val entry = if (targetId in entries) entries[targetId] else {
            val emptyEntry = BlocklistEntry(targetId, targetType, mutableListOf(), mutableListOf())
            entries.put(targetId, emptyEntry)
            emptyEntry
        }
        val blockedFeatures = entry.blockedFeatures
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
        saveEntries()
    }

    fun unblockFeature(targetId: Long, featureType: FeatureType) {
        val blockedFeatures = entries[targetId]?.blockedFeatures ?: return
        synchronized(blockedFeatures) {
            blockedFeatures.removeIf { it.feature == featureType }
        }
        saveEntries()
    }

    fun isFeatureBlocked(targetId: Long, featureType: FeatureType): Boolean {
        val blockedFeature = entries[targetId]?.let { entry: BlocklistEntry ->
            synchronized(entry.blockedFeatures) {
                entry.blockedFeatures.firstOrNull { it.feature == featureType }
            }
        } ?: return false
        // By checking and unblocking the feature here, we
        // don't have to set up a service to check it
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        if (blockedFeature.expiresAt != 0L && currentTimeSeconds >= blockedFeature.expiresAt) {
            unblockFeature(targetId, featureType)
            return false
        }
        return true
    }

}
