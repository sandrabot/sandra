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

import com.sandrabot.sandra.config.RedisConfig
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

/**
 * Responsible for managing connections to the redis database and manipulating records.
 */
class RedisManager(config: RedisConfig) {

    private val pool = JedisPool(
        JedisPoolConfig(), config.host, config.port, config.timeout, config.password, config.database
    )

    fun shutdown() = pool.destroy()

    fun <T> use(block: Jedis.() -> T) = pool.resource.use { block(it) }

    operator fun get(key: String): String? = use { get(key) }

    operator fun set(key: String, value: String): Unit = use { set(key, value) }

    operator fun minus(key: String): Unit = use { del(key) }

}
