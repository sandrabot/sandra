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
import com.beust.klaxon.json
import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.RedisPrefix
import com.sandrabot.sandra.entities.Configuration
import com.sandrabot.sandra.entities.Service
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import java.io.StringReader
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Stores configuration objects in memory to prevent overhead of recreating objects too often.
 */
class ConfigurationManager(private val sandra: Sandra) : Service(5) {

    private val klaxon = Klaxon()
    private val accessedKeys = mutableSetOf<Long>()
    private val cache: ExpiringMap<Long, Configuration> = ExpiringMap.builder()
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
            val configuration = cache[key]
            val prefix = when (configuration) {
                is GuildConfig -> RedisPrefix.GUILD
                is UserConfig -> RedisPrefix.USER
                else -> throw AssertionError()
            }
            sandra.redis["$prefix$key"] = klaxon.toJsonString(configuration)
        }
    }

    fun countGuilds() = cache.count { it.value is GuildConfig }
    fun countUsers() = cache.count { it.value is UserConfig }

    fun getGuild(id: Long) = get(id, GuildConfig::class, RedisPrefix.GUILD)
    fun getUser(id: Long) = get(id, UserConfig::class, RedisPrefix.USER)

    @Suppress("UNCHECKED_CAST")
    fun <T : Configuration> get(id: Long, type: KClass<T>, redisPrefix: RedisPrefix): T {
        return ((cache[id] ?: getOrDefault(id, type, redisPrefix)) as T).also {
            synchronized(accessedKeys) {
                accessedKeys.add(id)
            }
        }
    }

    private fun getOrDefault(
        id: Long, type: KClass<out Configuration>, redisPrefix: RedisPrefix
    ): Configuration {
        val json = sandra.redis["$redisPrefix$id"]?.let {
            klaxon.parseJsonObject(StringReader(it))
        } ?: json { obj("id" to id) }
        return (klaxon.fromJsonObject(json, type.java, type) as Configuration).also { cache[id] = it }
    }

}
