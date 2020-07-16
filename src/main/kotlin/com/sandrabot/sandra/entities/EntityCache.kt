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

package com.sandrabot.sandra.entities

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.RedisPrefix
import java.util.concurrent.TimeUnit

/**
 * Stores [T] objects in memory to mitigate any redis latency after the initial load.
 */
abstract class EntityCache<T : DataHolder>(private val sandra: Sandra, private val redisPrefix: RedisPrefix) {

    private val parser = Parser.default()
    private val cache: LoadingCache<Long, T> = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS).build(
                    object : CacheLoader<Long, T>() {
                        override fun load(key: Long): T {
                            val rawData = sandra.redis.get("$redisPrefix$key")
                            val data = if (rawData == null) JsonObject() else {
                                parser.parse(StringBuilder(rawData)) as JsonObject
                            }
                            return createEntity(key, data)
                        }
                    }
            )

    abstract fun createEntity(key: Long, data: JsonObject): T

    fun get(key: Long): T = cache.getUnchecked(key)

    fun save(key: Long, entity: T) {
        sandra.cacheExecutor.execute {
            val data = entity.toData()
            val redisKey = "$redisPrefix$key"
            if (data.isEmpty()) {
                // Delete empty objects to keep the database clean
                // This also ensures the change that removed the last key is persisted
                sandra.redis.delete(redisKey)
            } else {
                sandra.redis.set(redisKey, data.toJsonString())
            }
        }
    }

}
