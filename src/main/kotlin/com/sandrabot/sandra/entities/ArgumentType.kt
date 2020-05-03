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

/**
 * Tokens used to resolve text into objects for consumption by commands.
 */
enum class ArgumentType {

    /**
     * Searches for text channels in guilds.
     */
    CHANNEL,

    /**
     * Searches for command by name or alias.
     */
    COMMAND,

    /**
     * Searches for any digits that fit into a [Long].
     */
    DIGIT,

    /**
     * Searches for timestamps and resolves them to seconds that fit into a [Long].
     */
    DURATION,

    /**
     * Searches for emotes in guilds.
     */
    EMOTE,

    /**
     * Searches for optional command arguments prefixed with an exclamation mark.
     * Flags cannot be required.
     */
    FLAG,

    /**
     * Searches for items by name.
     */
    ITEM,

    /**
     * Searches for roles in guilds.
     */
    ROLE,

    /**
     * Any remaining text from parsing is resolved.
     * Typically this is used at the end of argument tokens.
     */
    TEXT,

    /**
     * Searches for a user.
     */
    USER,

    /**
     * Searches for voice channels in guilds.
     */
    VOICE,

    /**
     * Used to represent invalid argument types.
     */
    UNKNOWN;

    companion object {

        fun fromName(name: String): ArgumentType {
            return values().firstOrNull { name.equals(it.name, ignoreCase = true) } ?: UNKNOWN
        }

    }

}
