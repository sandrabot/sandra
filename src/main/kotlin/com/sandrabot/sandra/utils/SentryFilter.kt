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

package com.sandrabot.sandra.utils

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

/**
 * This class is used to filter events sent to Sentry and
 * prevent reporting network errors that we can not control.
 */
class SentryFilter : Filter<ILoggingEvent>() {

    override fun decide(event: ILoggingEvent): FilterReply {
        val messages = mutableListOf<String>()
        // Add all the throwable messages into a list
        if (event.message != null) messages.add(event.message)
        var proxy = event.throwableProxy
        while (proxy != null) {
            if (proxy.message != null) messages.add(proxy.message)
            proxy = proxy.cause
        }
        // Check if any of the messages contain any of our keywords
        return if (messages.any { m -> keywords.any { m.contains(it, ignoreCase = true) } }) {
            FilterReply.DENY
        } else FilterReply.NEUTRAL
    }

    companion object {
        private val keywords = arrayOf("timeout", "timed out")
    }

}
