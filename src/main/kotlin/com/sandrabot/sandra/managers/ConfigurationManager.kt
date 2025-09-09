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
import com.sandrabot.sandra.config.Configuration
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.RedisPrefix
import com.sandrabot.sandra.entities.Service
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.jodah.expiringmap.ExpirationListener
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

/**
 * Retrieves, stores, and caches configuration data for users and guilds.
 *
 * Whenever a configuration is accessed, it is assumed that the data may have been updated by the calling code.
 * Every 30 seconds, the recently accessed records will be sent to redis for storage.
 *
 * Objects expire from cache **1 hour** after they were last accessed to reduce the memory footprint.
 */
class ConfigurationManager(private val sandra: Sandra) : Service(30.seconds), ExpirationListener<Long, Configuration> {

    private val json = Json { encodeDefaults = true }
    private val accessedKeys = mutableSetOf<Long>()
    private val configMap: ExpiringMap<Long, Configuration> =
        ExpiringMap.builder().expirationPolicy(ExpirationPolicy.ACCESSED).expiration(1, TimeUnit.HOURS)
            .asyncExpirationListener(this).build()

    init {
        start() // immediately start the service
    }

    override fun shutdown() {
        super.shutdown() // shutdown the service and cancel the job
        runBlocking { execute() } // save any leftover keys one last time
    }

    override suspend fun execute() = synchronized(accessedKeys) {
        // create a copy of the list before clearing it and releasing the lock
        accessedKeys.toList().also { accessedKeys.clear() }
    }.forEach { store(it, configMap[it] ?: return@forEach) }

    override fun expired(key: Long, value: Configuration) = store(key, value)

    private fun store(id: Long, config: Configuration) {
        sandra.redis[prefix(config::class) + "$id"] = json.encodeToString(config)
    }

    fun getGuild(id: Long): GuildConfig = get(id)
    fun getUser(id: Long): UserConfig = get(id)

    operator fun get(guild: Guild) = getGuild(guild.idLong)
    operator fun get(user: User) = getUser(user.idLong)

    private inline fun <reified T : Configuration> get(id: Long): T = configMap.getOrPut(id) {
        sandra.redis[prefix(T::class) + "$id"]?.let { json.decodeFromString<T>(it) } ?: when (T::class) {
            GuildConfig::class -> GuildConfig(id)
            UserConfig::class -> UserConfig(id)
            else -> throw IllegalStateException("Illegal configuration type: ${T::class}")
        }
    }.also { synchronized(accessedKeys) { accessedKeys += id } } as T

    private fun <T : Configuration> prefix(clazz: KClass<T>) = when (clazz) {
        GuildConfig::class -> RedisPrefix.GUILD
        UserConfig::class -> RedisPrefix.USER
        else -> throw IllegalStateException("Illegal configuration type: $clazz")
    }

}
