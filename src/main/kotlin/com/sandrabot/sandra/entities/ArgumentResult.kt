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

import net.dv8tion.jda.api.entities.*
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

/**
 * Wrapper class for argument parsing results.
 * Type safety is achieved by calling the method you need,
 * however if the element is missing an exception will be thrown.
 */
class ArgumentResult(val results: Map<String, Any>) {

    /**
     * Used to check if an argument was parsed.
     */
    fun has(name: String): Boolean = name in results

    /**
     * Retrieve the array of type [T] with the name [name].
     * Note that despite the name, a [List] is returned.
     */
    inline fun <reified T> array(name: String): List<T> {
        return if (name in results) {
            (results[name] as List<*>).filterIsInstance<T>()
        } else emptyList()
    }

    /* Native Argument Objects */

    @ExperimentalTime
    fun duration(name: String) = digit(name).toDuration(TimeUnit.SECONDS)
    fun digit(name: String) = results[name] as Long
    fun  flag(name: String) = results[name] as Boolean
    fun  text(name: String) = results[name] as String

    /* Sandra Argument Objects */

    fun command(name: String) = results[name] as Command

    /* JDA Argument Objects */

    fun channel(name: String) = results[name] as TextChannel
    fun   voice(name: String) = results[name] as VoiceChannel
    fun   emote(name: String) = results[name] as Emote
    fun    role(name: String) = results[name] as Role
    fun    user(name: String) = results[name] as User

}
