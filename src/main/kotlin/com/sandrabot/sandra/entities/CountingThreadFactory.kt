/*
 *    Copyright 2017-2020 Avery Clifton and Logan Devecka
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sandrabot.sandra.entities

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

class CountingThreadFactory(private val identifier: String) : ThreadFactory {

    private val poolId: Long
    private var threadCount = AtomicLong(0)

    init {
        synchronized(identifiers) {
            val count = identifiers.computeIfAbsent(identifier) { AtomicLong(0) }
            poolId = count.getAndIncrement()
        }
    }

    override fun newThread(runnable: Runnable): Thread {
        val thread = Thread(runnable, "$identifier-$poolId-thread-${threadCount.incrementAndGet()}")
        thread.isDaemon = true
        return thread
    }

    companion object {
        private val identifiers = HashMap<String, AtomicLong>()
    }

}
