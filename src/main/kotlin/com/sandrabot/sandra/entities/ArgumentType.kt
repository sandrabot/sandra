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

/**
 * Tokens used to resolve text into objects for consumption by commands.
 * Type safety is achieved by using the corresponding method with the type name in [ArgumentResult].
 */
enum class ArgumentType {

    /**
     * Searches for text channels in guilds. Resolves [net.dv8tion.jda.api.entities.TextChannel] objects.
     */
    CHANNEL,

    /**
     * Searches for commands by name or alias. Resolves [com.sandrabot.sandra.entities.Command] objects.
     */
    COMMAND,

    /**
     * Searches for any digits that fit into a long. Resolves as a [kotlin.time.Duration].
     */
    DIGIT,

    /**
     * Searches for durations and converts them into seconds. Resolves as a [Long].
     */
    DURATION,

    /**
     * Searches for emotes in guilds. Resolves [net.dv8tion.jda.api.entities.Emote] objects.
     */
    EMOTE,

    /**
     * Searches for optional command arguments prefixed with an exclamation mark.
     * Flags cannot be required nor arrays.
     * Resolves as a [Boolean], whether the flag is present or not.
     */
    FLAG,

    /**
     * Searches for items by name. Resolves [com.sandrabot.sandra.entities.ItemType] objects.
     */
    ITEM,

    /**
     * Searches for roles in guilds. Resolves [net.dv8tion.jda.api.entities.Role] objects.
     */
    ROLE,

    /**
     * Any remaining text from parsing is consumed.
     * Text cannot be an array. Resolves as a [String].
     */
    TEXT,

    /**
     * Searches for mentioned users. Resolves [net.dv8tion.jda.api.entities.User] objects.
     */
    USER,

    /**
     * Searches for voice channels in guilds. Resolves [net.dv8tion.jda.api.entities.VoiceChannel] objects.
     */
    VOICE,

    /**
     * Used to represent invalid argument types.
     */
    UNKNOWN;

    companion object {

        fun fromName(name: String): ArgumentType {
            return values().find { name.equals(it.name, ignoreCase = true) } ?: UNKNOWN
        }

    }

}
