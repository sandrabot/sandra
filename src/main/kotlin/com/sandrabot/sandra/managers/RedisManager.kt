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

import com.sandrabot.sandra.config.RedisConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.RedisClient

/**
 * Responsible for managing connections to the redis database and manipulating records.
 */
class RedisManager(config: RedisConfig) {

    private val client: RedisClient

    init {
        val clientConfig = DefaultJedisClientConfig.builder()
            .user(config.user).password(config.password)
            .database(config.database).clientName("sandra").build()
        client = RedisClient.builder().hostAndPort(config.host, config.port).clientConfig(clientConfig).build()
    }

    fun shutdown(): Unit = client.close()

    fun ping(): String = client.ping()

    operator fun get(key: String): String? = client.get(key)

    operator fun set(key: String, value: String): String = client.set(key, value)

    operator fun minus(key: String): Long = client.del(key)

    operator fun contains(key: String): Boolean = client.exists(key)

}
