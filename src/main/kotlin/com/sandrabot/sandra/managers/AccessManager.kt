/*
 * Copyright 2017-2026 Avery Carroll and Logan Devecka
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
import com.sandrabot.sandra.entities.FeatureFlag
import kotlinx.serialization.json.Json

class AccessManager(private val sandra: Sandra) {

    private val disabledFeatures = sandra.settings.features.disabledFeatures.toMutableSet()
    private val restrictedMap: MutableMap<Long, MutableSet<FeatureFlag>> =
        sandra.redis[RedisPrefix.SETTING + "access"]?.let { Json.decodeFromString(it) } ?: mutableMapOf()

    fun isAccessRevoked(id: Long, feature: FeatureFlag): Boolean {
        val set = restrictedMap[id] ?: return false
        return feature in set || FeatureFlag.ALL in set
    }

    fun isFeatureDisabled(feature: FeatureFlag): Boolean {
        return feature in disabledFeatures || FeatureFlag.ALL in disabledFeatures
    }

    fun isRestricted(id: Long) = id in restrictedMap

    fun grant(id: Long, feature: FeatureFlag): Boolean {
        val set = restrictedMap[id] ?: return false
        return set.remove(feature).also { if (set.isEmpty()) restrictedMap.remove(id) }
    }

    fun revoke(id: Long, feature: FeatureFlag): Boolean {
        return restrictedMap.getOrPut(id) { mutableSetOf() }.add(feature)
    }

    fun enableFeature(feature: FeatureFlag) = disabledFeatures.remove(feature)
    fun disableFeature(feature: FeatureFlag) = disabledFeatures.add(feature)

    fun shutdown() {
        if (restrictedMap.isEmpty()) return
        sandra.redis[RedisPrefix.SETTING + "access"] = Json.encodeToString(restrictedMap)
    }

}
