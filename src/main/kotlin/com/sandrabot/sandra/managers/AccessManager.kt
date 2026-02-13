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

/**
 * Manages granular access to features throughout the bot.
 */
class AccessManager(private val sandra: Sandra) {

    /**
     * Creates a mutable copy of features that are disabled in the config file.
     * Any changes made to this collection will not be reflected at the next startup.
     */
    val disabledFeatures = sandra.settings.features.disabledFeatures.toMutableSet()

    /**
     * This map keeps track of the users or guilds that have been restricted.
     * Restrictions are automatically loaded from the database while starting up.
     * Any changes made to the database while running will not be reflected and may be overwritten.
     *
     * @see isRestricted
     * @see grant
     * @see revoke
     */
    private val restrictedMap: MutableMap<Long, MutableSet<FeatureFlag>> by lazy {
        sandra.redis[REDIS_ACCESS]?.let { Json.decodeFromString(it) } ?: mutableMapOf()
    }

    /**
     * Returns `true` when access to the `feature` has been revoked from this `id`.
     */
    fun isAccessRevoked(id: Long, feature: FeatureFlag): Boolean {
        val set = restrictedMap[id] ?: return false
        return feature in set || FeatureFlag.ALL in set
    }

    /**
     * Returns `true` when this `feature` has been disabled globally.
     */
    fun isFeatureDisabled(feature: FeatureFlag): Boolean {
        return feature in disabledFeatures || FeatureFlag.ALL in disabledFeatures
    }

    /**
     * Returns `true` if this `id` maintains any restrictions whatsoever.
     */
    fun isRestricted(id: Long) = id in restrictedMap

    /**
     * Restores access to `feature` for this `id`. Returns `true` if changes were made.
     */
    fun grant(id: Long, feature: FeatureFlag): Boolean {
        val set = restrictedMap[id] ?: return false
        return set.remove(feature).also { if (set.isEmpty()) restrictedMap.remove(id) }
    }

    /**
     * Revokes access to `feature` for this `id`. Returns `true` if changes were made.
     */
    fun revoke(id: Long, feature: FeatureFlag): Boolean {
        return restrictedMap.getOrPut(id) { mutableSetOf() }.add(feature)
    }

    /**
     * Save current restrictions to the database if applicable.
     */
    fun shutdown() {
        if (restrictedMap.isEmpty()) sandra.redis - REDIS_ACCESS else
        sandra.redis[REDIS_ACCESS] = Json.encodeToString(restrictedMap)
    }

    private companion object {
        val REDIS_ACCESS = RedisPrefix.SETTING + "access"
    }

}
