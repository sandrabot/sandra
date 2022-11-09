/*
 * Copyright 2017-2022 Avery Carroll and Logan Devecka
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

import com.sandrabot.sandra.exceptions.MissingTranslationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import net.dv8tion.jda.api.interactions.DiscordLocale

/**
 * Deals with loading and retrieving translation keys from the language files.
 */
@OptIn(ExperimentalSerializationApi::class)
class TranslationManager {

    private val translations: Map<DiscordLocale, Map<String, Any>>

    /**
     * A collection of locales that have been loaded and are available for use.
     */
    val availableLocales: Set<DiscordLocale>

    init {
        // read entry file names using streams and line breaks
        translations = object {}.javaClass.classLoader.getResourceAsStream("translations").use { stream ->
            String(stream?.readBytes() ?: throw IllegalStateException("Translation directory does not exist"))
        }.lines().filterNot { it.isBlank() }.associate { path ->
            // this is the unique identifier for the specific language and dialect
            val identifier = path.substringBefore('.').replace('_', '-')
            val content: JsonObject = try {
                val stream = object {}.javaClass.classLoader.getResourceAsStream("translations/$path")
                    ?: throw IllegalStateException("Failed to read translation file for $identifier")
                Json.decodeFromStream(stream)
            } catch (t: Throwable) {
                throw IllegalStateException("Failed to parse translation file for $identifier", t)
            }
            val pathMap = mutableMapOf<String, Any>()
            // recursively load all translation paths and values into maps
            loadRecursive("", pathMap, content)
            DiscordLocale.from(identifier) to pathMap
        }
        availableLocales = translations.keys
    }

    private fun loadRecursive(
        root: String, pathMap: MutableMap<String, Any>, jsonObject: JsonObject
    ): Unit = jsonObject.entries.forEach { (key, value) ->
        val path = if (root.isEmpty()) key else "$root.$key"
        when (value) {
            is JsonObject -> loadRecursive(path, pathMap, value)
            is JsonPrimitive -> pathMap[path] = value.content
            is JsonArray -> pathMap[path] = value.jsonArray.map { it.jsonPrimitive.content }.toList()
        }
    }

    private fun getAny(locale: DiscordLocale, path: String): Any {
        // if this locale isn't available, fall back to the default instead of throwing
        val translation = translations[if (locale in translations) locale else DiscordLocale.ENGLISH_US]
            ?: throw AssertionError("Missing default translation map")
        return translation[path] ?: throw MissingTranslationException("Missing translation path $path for $locale")
    }

    /**
     * Allows you to retrieve the entire list of possible keys for this path.
     */
    fun getList(locale: DiscordLocale, path: String): List<String> = getAny(locale, path).let {
        if (it is List<*>) it.filterIsInstance<String>() else {
            throw IllegalArgumentException("Translation path $path for $locale is not a list")
        }
    }

    /**
     * Retrieves the translation key located at [path].
     * If the target is a list, a random entry from the list will be returned.
     */
    fun get(locale: DiscordLocale, path: String): String = when (val value = getAny(locale, path)) {
        is String -> value
        is List<*> -> value.random() as String
        else -> throw AssertionError("Translation path $path for $locale is type ${value::class}")
    }

}
