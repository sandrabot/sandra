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

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * A wrapper class for running periodic tasks.
 */
abstract class Service(private val period: Long) {

    val isRunning: Boolean
        get() = !(task?.isDone ?: true)

    private var task: ScheduledFuture<*>? = null

    protected abstract fun execute()

    open fun start() = beginTask()
    open fun shutdown() {
        task?.cancel(false)
    }

    private fun beginTask() {
        if (isRunning) shutdown()
        task = executor.scheduleAtFixedRate(::execute, period, period, TimeUnit.SECONDS)
    }

    companion object {
        // Services aren't time critical, if two run at the same time, one can wait
        private val executor = Executors.newSingleThreadScheduledExecutor(CountingThreadFactory("service"))
    }

}
