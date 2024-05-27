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

import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration

/**
 * Utility class that periodically executes a task using coroutines.
 *
 * The [interval] is the duration between each execution of the task.
 * You may delay the first execution of the task by using [initialDelay]. By default, it is the same value as [interval].
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class Service(
    private val interval: Duration, private val initialDelay: Duration = interval,
) {

    private var job: Job? = null

    /**
     * Used to determine if the service is currently running.
     */
    val isActive: Boolean
        get() = job?.isActive ?: false

    /**
     * This method defines the task that should be executed every [interval] by the service.
     */
    protected abstract suspend fun execute()

    /**
     * Can be used to modify the startup behavior of the service.
     * This method does nothing if the service is already running.
     *
     * **Warning:** You must always call `super.start()` to actually start the service.
     */
    open fun start() {
        if (isActive) return
        job = serviceScope.launch {
            delay(initialDelay)
            while (isActive) try {
                execute()
                delay(interval)
            } catch (_: CancellationException) {
                // these can be safely ignored, only occurs when service shuts down
            } catch (t: Throwable) {
                logger.error("An exception occurred while executing a service task, halting service", t)
                shutdown()
            }
        }
    }

    /**
     * Can be used to modify the shutdown behavior of the service.
     * This method does nothing if the service is not running.
     *
     * **Warning:** You must always call `super.shutdown()` to actually stop the service.
     */
    open fun shutdown() {
        job?.cancel("Service is shutting down")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Service::class.java)
        private val serviceScope = CoroutineScope(Dispatchers.Default)
    }

}
