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

import com.sandrabot.sandra.constants.Emotes
import kotlin.reflect.KClass

enum class Category(val emote: String) {

    CUSTOM(Emotes.PATREON),
    ESSENTIAL(Emotes.PIN),
    FUN(Emotes.FAVORITE),
    GAME(Emotes.CREDIT),
    MODERATION(Emotes.MOD_SHIELD),
    MUSIC(Emotes.MUSIC),
    SOCIAL(Emotes.USER),
    UTILITY(Emotes.PROMPT);

    val displayName = name.toLowerCase().capitalize()

    companion object {
        fun fromClass(clazz: KClass<out Command>): Category? {
            val qualifiedName = clazz.qualifiedName
                    ?: throw IllegalArgumentException("Local or anonymous commands are not permitted")
            if (qualifiedName.substringAfterLast(".").contains("$")) return null
            val packageName = qualifiedName.substringBeforeLast(".").substringAfterLast(".")
            return values().find { packageName.equals(it.name, ignoreCase = true) }
        }
    }

}
