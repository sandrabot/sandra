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

package com.sandrabot.sandra.entities

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

/**
 * A simple implementation of a rate limiter that
 * only allows a certain number of calls per second.
 */
class SimpleRateLimiter(callsPerSecond: Double) {

    private val mutex = Mutex()

    @Volatile
    private var next: Long = Long.MIN_VALUE
    private val delayNanos: Long = (1_000_000_000 / callsPerSecond).toLong()

    /**
     * Acquires a permit from the rate limiter, blocking until one is available.
     */
    suspend fun acquire() {
        val now = System.nanoTime()
        val until = mutex.withLock {
            max(next, now).also {
                next = it + delayNanos
            }
        }
        if (until != now) {
            delay((until - now) / 1_000_000)
        }
    }

}
