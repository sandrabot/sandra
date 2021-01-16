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

import com.sandrabot.sandra.entities.CachedMessage
import net.dv8tion.jda.api.entities.Message
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import java.util.concurrent.TimeUnit

class MessageManager {

    private val cache: ExpiringMap<Long, CachedMessage> = ExpiringMap.builder()
        .expirationPolicy(ExpirationPolicy.CREATED)
        .expiration(6, TimeUnit.HOURS).build()

    val size: Int
        get() = cache.size

    fun put(message: Message) {
        cache[message.idLong] = CachedMessage(message)
    }

    fun remove(messageId: Long) = cache.remove(messageId)

    fun filter(filter: (CachedMessage) -> Boolean) = cache.values.filter(filter)

}
