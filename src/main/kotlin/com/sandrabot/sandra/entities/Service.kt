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

package com.sandrabot.sandra.entities

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/**
 * Utility class for running periodic tasks throughout the bot.
 *
 * @param interval The interval in seconds between each execution of the task.
 * @param initialDelay The initial delay in seconds before the task is executed for the first time.
 */
abstract class Service(private val interval: Long, private val initialDelay: Long = interval) {

    private var job: Job? = null

    val isActive: Boolean
        get() = job?.isActive ?: false

    protected abstract suspend fun execute()

    /**
     * Starts the service and executes the task every [interval] seconds.
     * If the service is already running, this method will do nothing.
     */
    open fun start() {
        if (isActive) return
        serviceScope.launch {
            delay(initialDelay.seconds)
            while (isActive) try {
                execute()
                delay(interval.seconds)
            } catch (t: Throwable) {
                logger.error("An exception occurred while executing a service task, halting service", t)
                shutdown()
            }
        }
    }

    /**
     * Stops the service and cancels the task.
     */
    open fun shutdown() {
        job?.cancel("Service shutdown")
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(Service::class.java)
        internal val serviceScope = CoroutineScope(Dispatchers.Default)
    }

}
