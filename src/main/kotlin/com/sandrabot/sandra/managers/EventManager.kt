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

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.IEventManager
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A typical [IEventManager] intended for future customization.
 */
class EventManager : IEventManager {

    private val listeners = CopyOnWriteArrayList<EventListener>()

    fun registerAll(vararg listeners: Any) {
        listeners.forEach { register(it) }
    }

    override fun register(listener: Any) {
        if (listener is EventListener) {
            listeners.add(listener)
        } else throw IllegalArgumentException("$listener does not implement EventListener")
    }

    override fun unregister(listener: Any) {
        listeners.remove(listener)
    }

    override fun getRegisteredListeners(): MutableList<Any> {
        return Collections.unmodifiableList(listeners)
    }

    override fun handle(event: GenericEvent) {
        for (it in listeners) {
            try {
                it.onEvent(event)
            } catch (throwable: Throwable) {
                logger.error("One of the event listeners threw an uncaught exception", throwable)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EventManager::class.java)
    }

}
