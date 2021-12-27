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

package com.sandrabot.sandra.entities

import net.dv8tion.jda.api.interactions.commands.OptionType

/**
 * These types are used to represent object types for arguments consumed by commands.
 * Type safety is achieved by using the corresponding method with the type name in [ArgumentResult].
 */
enum class ArgumentType(val optionType: OptionType) {

    /**
     * Resolves a [Boolean] option.
     */
    BOOLEAN(OptionType.BOOLEAN),

    /**
     * Searches for command categories. Resolves as a [Category].
     */
    CATEGORY(OptionType.STRING),

    /**
     * Resolves [net.dv8tion.jda.api.entities.TextChannel] objects.
     */
    CHANNEL(OptionType.CHANNEL),

    /**
     * Searches for commands by path. Resolves as a [Command].
     */
    COMMAND(OptionType.STRING),

    /**
     * Resolves any number between -2^53 and 2^53 as a [Double].
     */
    DOUBLE(OptionType.NUMBER),

    /**
     * Searches for durations. Resolves as a [kotlin.time.Duration].
     */
    DURATION(OptionType.STRING),

    /**
     * Searches for emotes in guilds. Resolves [net.dv8tion.jda.api.entities.Emote] objects.
     */
    EMOTE(OptionType.STRING),

    /**
     * Resolves any integer between -2^53 and 2^53 as a [Long].
     */
    INTEGER(OptionType.INTEGER),

    /**
     * Resolves mentionable entities [net.dv8tion.jda.api.entities.Role] and [net.dv8tion.jda.api.entities.User].
     */
    MENTIONABLE(OptionType.MENTIONABLE),

    /**
     * Resolves [net.dv8tion.jda.api.entities.NewsChannel] objects.
     */
    NEWS(OptionType.CHANNEL),

    /**
     * Resolves [net.dv8tion.jda.api.entities.Role] objects.
     */
    ROLE(OptionType.ROLE),

    /**
     * Resolves [net.dv8tion.jda.api.entities.StageChannel] objects.
     */
    STAGE(OptionType.CHANNEL),

    /**
     * Resolves a [String] option.
     */
    TEXT(OptionType.STRING),

    /**
     * Resolves [net.dv8tion.jda.api.entities.User] objects.
     */
    USER(OptionType.USER),

    /**
     * Resolves [net.dv8tion.jda.api.entities.VoiceChannel] objects.
     */
    VOICE(OptionType.CHANNEL);

    companion object {
        fun fromName(name: String): ArgumentType {
            return values().firstOrNull { name.equals(it.name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown argument type with name $name")
        }
    }

}
