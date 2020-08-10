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

import com.sandrabot.sandra.entities.IgnoreEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.coroutines.EmptyCoroutineContext

/**
 * An intermediary handler that dispatches events generated by JDA and Sandra.
 *
 * The concept to register listeners by method parameter types was used from
 * the AutoListener written by MinnDevelopment and adapted for use within Sandra.
 */
class EventManager : EventListener {

    private val listeners = ConcurrentHashMap<Class<*>, MutableSet<Pair<Method, Any>>>()
    private val scope = CoroutineScope(EmptyCoroutineContext)

    fun register(vararg registrants: Any) {
        for (listener in registrants) {
            for (method in listener::class.java.declaredMethods) {
                // Ignore any methods that aren't public members
                if (method.modifiers and (Modifier.STATIC or Modifier.PUBLIC) != Modifier.PUBLIC) continue
                // Ignore any methods with more or less than 1 parameter
                if (method.parameterCount != 1) continue
                // Ignore any methods that are explicitly ignored
                if (method.getAnnotation(IgnoreEvent::class.java) != null) continue
                val pairs = listeners.computeIfAbsent(method.parameterTypes[0]) { CopyOnWriteArraySet() }
                pairs.add(method to listener)
            }
        }
    }

    fun unregister(registrant: Any) {
        listeners.forEach { (clazz, list) ->
            val target = registrant::class.java
            list.removeIf { target.isInstance(it.second) }
            if (list.isEmpty()) listeners.remove(clazz)
        }
    }

    override fun onEvent(event: GenericEvent) = handleEvent(event)

    fun handleEvent(event: Any) {
        for (entry in listeners) {
            val eventType: Class<*> = entry.key
            if (eventType.isInstance(event)) {
                scope.launch {
                    val registeredMethods: Set<Pair<Method, Any>> = entry.value
                    registeredMethods.forEach { dispatch(event, it.first, it.second) }
                }
            }
        }
    }

    private fun dispatch(event: Any, method: Method, instance: Any) {
        try {
            method.invoke(instance, event)
        } catch (e: IllegalAccessException) {
            logger.error("Failed to invoke $method for event dispatch", e)
        } catch (e: InvocationTargetException) {
            logger.error("One of the event listeners threw an uncaught exception", e.cause)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EventManager::class.java)
    }

}
