/*
 * Copyright 2026 Avery Carroll, Logan Devecka, and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandrabot.sandra.constants

import com.sandrabot.sandra.entities.Command
import net.dv8tion.jda.api.entities.emoji.Emoji
import kotlin.reflect.KClass

enum class Category(emote: String) {

    CUSTOM(Emojis.DONATE),
    ESSENTIAL(Emojis.PIN),
    FUN(Emojis.FUN),
    GAME(Emojis.CASH),
    LASTFM(Emojis.LASTFM),
    MODERATION(Emojis.MOD),
    MUSIC(Emojis.MUSIC),
    OWNER(Emojis.CONFIG),
    SOCIAL(Emojis.USER),
    UTILITY(Emojis.PROMPT);

    val displayName = name.lowercase()
    val emoji = Emoji.fromFormatted(emote)

    companion object {
        fun fromClass(clazz: KClass<out Command>): Category {
            if (clazz.qualifiedName?.startsWith("com.sandrabot.sandra.commands.") == false)
                throw IllegalArgumentException("Class must be a member of a command category package")
            val packageName = clazz.toString().substringBeforeLast(".").substringAfterLast(".")
            return entries.first { packageName.equals(it.name, ignoreCase = true) }
        }
    }

}
