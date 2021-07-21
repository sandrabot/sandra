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
import com.sandrabot.sandra.config.Configuration
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.RedisPrefix
import com.sandrabot.sandra.entities.Service
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.findAnnotation

/**
 * Stores configuration objects in memory to prevent overhead of recreating objects too often.
 */
class ConfigurationManager(private val sandra: Sandra) : Service(30) {

    private val json = Json { encodeDefaults = true }
    private val accessedKeys = mutableSetOf<Long>()
    private val configs: ExpiringMap<Long, Configuration> = ExpiringMap.builder()
        .expirationPolicy(ExpirationPolicy.ACCESSED)
        .expiration(1, TimeUnit.DAYS)
        .build()

    init {
        start()
    }

    override fun shutdown() {
        super.shutdown()
        execute()
    }

    override fun execute() {
        val copyOfKeys = synchronized(accessedKeys) {
            LinkedList(accessedKeys).also { accessedKeys.clear() }
        }
        for (key in copyOfKeys) {
            val configuration = configs[key]
            val prefix = when (configuration) {
                is GuildConfig -> RedisPrefix.GUILD
                is UserConfig -> RedisPrefix.USER
                else -> throw AssertionError()
            }
            sandra.redis["$prefix$key"] = json.encodeToString(configuration)
        }
    }

    fun countGuilds() = configs.count { it.value is GuildConfig }
    fun countUsers() = configs.count { it.value is UserConfig }

    fun getGuild(id: Long) = get(id, RedisPrefix.GUILD) as GuildConfig
    fun getUser(id: Long) = get(id, RedisPrefix.USER) as UserConfig

    fun get(id: Long, redisPrefix: RedisPrefix): Configuration {
        return (configs[id] ?: getOrDefault(id, redisPrefix)).also {
            synchronized(accessedKeys) { accessedKeys.add(id) }
        }
    }

    private fun getOrDefault(id: Long, redisPrefix: RedisPrefix): Configuration {
        val jsonString = sandra.redis["$redisPrefix$id"] ?: run {
            val serialName = if (redisPrefix == RedisPrefix.GUILD) guildSerialName else userSerialName
            """{"type":"$serialName","id":$id}"""
        }
        return json.decodeFromString<Configuration>(jsonString).also { configs[id] = it }
    }

    companion object {
        private val guildSerialName = GuildConfig::class.findAnnotation<SerialName>()!!.value
        private val userSerialName = UserConfig::class.findAnnotation<SerialName>()!!.value
    }

}
