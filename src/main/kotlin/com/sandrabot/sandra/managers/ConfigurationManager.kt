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
import com.sandrabot.sandra.config.Configuration
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.RedisPrefix
import com.sandrabot.sandra.entities.Service
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.jodah.expiringmap.ExpirationListener
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import java.util.concurrent.TimeUnit

/**
 * Stores configuration objects in memory to prevent overhead of recreating objects too often.
 */
class ConfigurationManager(private val sandra: Sandra) : Service(30), ExpirationListener<Long, Configuration> {

    private val json = Json { encodeDefaults = true }
    private val accessedKeys = mutableSetOf<Long>()
    private val configs: ExpiringMap<Long, Configuration> =
        ExpiringMap.builder().expirationPolicy(ExpirationPolicy.ACCESSED).expiration(1, TimeUnit.HOURS)
            .asyncExpirationListener(this).build()

    init {
        start()
    }

    fun countGuilds() = configs.count { it.value is GuildConfig }
    fun countUsers() = configs.count { it.value is UserConfig }

    override fun shutdown() {
        super.shutdown()
        runBlocking { execute() }
    }

    override suspend fun execute() = synchronized(accessedKeys) {
        accessedKeys.toList().also { accessedKeys.clear() }
    }.forEach { store(it, configs[it] ?: return@forEach) }

    override fun expired(key: Long, value: Configuration) = store(key, value)

    private fun store(id: Long, config: Configuration) {
        val prefix = if (config is GuildConfig) RedisPrefix.GUILD else RedisPrefix.USER
        sandra.redis["$prefix$id"] = json.encodeToString(config)
    }

    fun getGuild(id: Long) = get<GuildConfig>(id)
    fun getUser(id: Long) = get<UserConfig>(id)

    private inline fun <reified T : Configuration> get(id: Long): T = configs.getOrPut(id) {
        val prefix = if (T::class == GuildConfig::class) RedisPrefix.GUILD else RedisPrefix.USER
        sandra.redis["$prefix$id"]?.let { json.decodeFromString<T>(it) } ?: when (T::class) {
            GuildConfig::class -> GuildConfig(id)
            UserConfig::class -> UserConfig(id)
            else -> throw AssertionError("Illegal configuration type")
        }
    }.also { synchronized(accessedKeys) { accessedKeys += id } } as T

}
