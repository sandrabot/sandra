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
import com.sandrabot.sandra.config.*
import com.sandrabot.sandra.constants.RedisPrefix
import com.sandrabot.sandra.entities.Service
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import java.util.*
import java.util.concurrent.TimeUnit

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

    @OptIn(ExperimentalSerializationApi::class)
    override fun execute() {
        val copyOfKeys = synchronized(accessedKeys) {
            LinkedList(accessedKeys).also { accessedKeys.clear() }
        }
        for (key in copyOfKeys) {
            when (val configuration = configs[key]) {
                is GuildConfig -> sandra.redis["${RedisPrefix.GUILD}$key"] =
                    json.encodeToString(ConfigTransformer, configuration)
                is UserConfig -> sandra.redis["${RedisPrefix.USER}$key"] =
                    json.encodeToString(ConfigTransformer, configuration)
                else -> {}
            }
        }
    }

    fun countGuilds() = configs.count { it.value is GuildConfig }
    fun countUsers() = configs.count { it.value is UserConfig }

    fun getGuild(id: Long) = get(id, RedisPrefix.GUILD) as GuildConfig
    fun getUser(id: Long) = get(id, RedisPrefix.USER) as UserConfig

    fun get(id: Long, redisPrefix: RedisPrefix) =
        (configs[id] ?: getOrDefault(id, redisPrefix)).also { synchronized(accessedKeys) { accessedKeys.add(id) } }

    @OptIn(ExperimentalSerializationApi::class)
    private fun getOrDefault(id: Long, redisPrefix: RedisPrefix) = (sandra.redis["$redisPrefix$id"]
        ?.let { json.decodeFromString(ConfigSerializer, it) } ?: when (redisPrefix) {
        RedisPrefix.GUILD -> GuildConfig(id)
        RedisPrefix.USER -> UserConfig(id)
        else -> throw AssertionError()
    }).also { configs[id] = it }

}
