/*
 * Copyright 2017-2024 Avery Carroll and Logan Devecka
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

import com.sandrabot.sandra.constants.Emotes
import kotlin.reflect.KClass

enum class Category(val emote: String) {

    CUSTOM(Emotes.PATREON),
    ESSENTIAL(Emotes.PIN),
    FUN(Emotes.FUN),
    GAME(Emotes.CASH),
    MODERATION(Emotes.MOD),
    MUSIC(Emotes.MUSIC),
    OWNER(Emotes.CONFIG),
    SOCIAL(Emotes.USER),
    UTILITY(Emotes.PROMPT);

    val displayName = name.lowercase()

    companion object {
        fun fromClass(clazz: KClass<out Command>): Category {
            if (clazz.qualifiedName?.startsWith("com.sandrabot.sandra.commands.") == false)
                throw IllegalArgumentException("Class must be a member of a command category package")
            val packageName = clazz.toString().substringBeforeLast(".").substringAfterLast(".")
            return entries.first { packageName.equals(it.name, ignoreCase = true) }
        }
    }

}
