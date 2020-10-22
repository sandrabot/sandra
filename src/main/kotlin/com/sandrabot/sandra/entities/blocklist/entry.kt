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

package com.sandrabot.sandra.entities.blocklist

private fun blockedFeature(entry: BlocklistEntry, featureType: FeatureType): BlockedFeature? {
    return synchronized(entry.blockedFeatures) {
        entry.blockedFeatures.find { it.feature == featureType }
    }
}

data class BlocklistEntry(
        val targetId: Long,
        val targetType: TargetType,
        val blockedFeatures: MutableList<BlockedFeature>,
        val offences: MutableList<BlocklistOffence>
) {

    fun getReason(featureType: FeatureType): String? {
        return synchronized(offences) {
            offences.find { featureType in it.features }
        }?.reason
    }

    fun isFeatureBlocked(featureType: FeatureType): Boolean {
        blockedFeature(this, featureType)?.apply {
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            val isExpired = expiresAt != 0L && currentTimeSeconds >= expiresAt
            if (isExpired) synchronized(blockedFeatures) {
                blockedFeatures.remove(this)
            }
            return !isExpired
        }
        return false
    }

    fun isNotified(featureType: FeatureType) = blockedFeature(this, featureType)?.notification != null

    fun recordNotify(featureType: FeatureType, channel: Long, message: Long) {
        blockedFeature(this, featureType)?.apply {
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            notification = FeatureNotification(currentTimeSeconds, channel, message)
        }
    }

}

data class BlockedFeature(
        val feature: FeatureType, val expiresAt: Long, var notification: FeatureNotification? = null
)

data class FeatureNotification(
        val timestamp: Long, val channel: Long, val message: Long
)

data class BlocklistOffence(
        val features: List<FeatureType>, val moderator: Long,
        val timestamp: Long, val automated: Boolean, val reason: String
)
