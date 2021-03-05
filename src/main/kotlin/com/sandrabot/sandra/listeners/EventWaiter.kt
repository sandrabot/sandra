/*
 * Copyright 2017-2021 Avery Carroll and Logan Devecka
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

package com.sandrabot.sandra.listeners

import com.sandrabot.sandra.entities.CountingThreadFactory
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReconnectedEvent
import net.dv8tion.jda.api.events.ShutdownEvent
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

class EventWaiter {

    private val executor = Executors.newSingleThreadScheduledExecutor(CountingThreadFactory("waiter"))
    private val waitingEvents = mutableMapOf<KClass<*>, MutableSet<WaitingEvent<*>>>()

    @Suppress("unused")
    fun onEvent(event: GenericEvent) {

        if (event is ReconnectedEvent) {
            // A ReconnectedEvent signifies that JDA entity objects were rebuilt
            // Clearing the waiting events removes all references to the old objects
            waitingEvents.clear()
            return
        } else if (event is ShutdownEvent) {
            // A ShutdownEvent signifies that JDA has shutdown and we should too
            executor.shutdown()
            return
        }

        var eventType: KClass<*> = event::class
        while (eventType != Event::class) {
            waitingEvents[eventType]?.let { eventSet ->
                synchronized(eventSet) {
                    // Prevent CME by removing successful attempts in bulk
                    val toRemove = eventSet.filter { it.attempt(event) }
                    eventSet.removeAll(toRemove)
                }
            }
            // Walk the class hierarchy to find generic events as well
            eventType = eventType.superclasses.first()
        }

    }

    /**
     * Waits for a single event of type [T] and executes [action] if [test] returns `true`.
     * If [timeout] and [unit] are not null and larger than 0, events may expire.
     * When an event expires, [expired] will be executed instead if not null.
     */
    fun <T : GenericEvent> waitForEvent(
        eventType: KClass<T>, timeout: Long = 0, unit: TimeUnit? = null,
        expired: (() -> Unit)? = null, test: (T) -> Boolean, action: (T) -> Unit
    ) {
        val waitingEvent = WaitingEvent(test, action)
        val eventSet = waitingEvents.computeIfAbsent(eventType) { mutableSetOf() }
        // Prevent CME by synchronizing any edits to the event set
        synchronized(eventSet) { eventSet.add(waitingEvent) }
        // Only schedule if both the timeout and unit are valid
        if (timeout > 0 && unit != null) {
            executor.schedule({
                synchronized(eventSet) {
                    eventSet.remove(waitingEvent)
                }.also { if (it && expired != null) expired() }
            }, timeout, unit)
        }
    }

    private class WaitingEvent<T : GenericEvent>(
        private val test: (T) -> Boolean,
        private val consumer: (T) -> Unit
    ) {

        @Suppress("UNCHECKED_CAST")
        fun attempt(event: GenericEvent): Boolean = test(event as T).also { if (it) consumer(event) }

    }

}
