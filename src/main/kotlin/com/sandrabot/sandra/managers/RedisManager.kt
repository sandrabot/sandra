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

import com.sandrabot.sandra.config.RedisConfig
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

/**
 * This class provides a means of communication with a redis server.
 */
class RedisManager(config: RedisConfig) {

    private val pool = JedisPool(JedisPoolConfig(), config.host, config.port, config.timeout, config.password, config.database)

    val resource: Jedis
        get() = pool.resource

    fun get(key: String): String? {
        return resource.use { it.get(key) }
    }

    fun delete(key: String) {
        resource.use { it.del(key) }
    }

    fun set(key: String, value: String) {
        resource.use { it.set(key, value) }
    }

    fun shutdown() {
        pool.destroy()
    }

}
