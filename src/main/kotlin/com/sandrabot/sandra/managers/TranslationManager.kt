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
import com.sandrabot.sandra.utils.getResourceAsText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.io.File
import java.util.*

/**
 * Deals with loading and retrieving translation keys from the language files.
 */
class TranslationManager {

    private val translations: Map<Locale, Map<String, Any>>

    /**
     * A collection of locales that have been loaded and are available for use.
     */
    val availableLocales: Set<Locale>

    init {
        // start loading the translation files by getting a list of resource files
        val directoryPath = object {}.javaClass.getResource("/translations")?.file
        val translationDir = File(directoryPath ?: throw IllegalStateException("Translation directory is missing"))
        if (!translationDir.isDirectory) throw IllegalStateException("$translationDir is not a directory")
        translations = translationDir.listFiles()?.associate { file ->
            // this is the identifier used by other applications to describe this locale
            val identifier = file.nameWithoutExtension.replace('_', '-')
            val jsonObject: JsonObject = try {
                // since we don't know the *exact* path for this file, we need to ask for it
                // this allows us to load it regardless of how the code is executed
                val path = file.toRelativeString(translationDir.parentFile)
                val translationJson = getResourceAsText("/$path")
                    ?: throw IllegalStateException("File for $identifier is missing, expected at $path")
                Json.decodeFromString(translationJson)
            } catch (t: Throwable) {
                throw IllegalStateException("Failed to parse translation file for $identifier", t)
            }
            val pathMap = mutableMapOf<String, Any>()
            // recursively load all translation paths and values into maps
            loadRecursive("", pathMap, jsonObject)
            Locale.forLanguageTag(identifier) to pathMap
        } ?: throw IllegalStateException("Failed to initialize translations")
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

    private fun getAny(locale: Locale, path: String): Any {
        // if this locale isn't available, fall back to the default instead of throwing
        val translation = translations[if (locale in translations) locale else Locale.US]
            ?: throw AssertionError("Missing translation map for $locale and ${Locale.US}")
        return translation[path] ?: throw MissingTranslationException("Missing translation path $path for $locale")
    }

    /**
     * Allows you to retrieve the entire list of possible keys for this path.
     */
    fun getList(locale: Locale, path: String): List<String> = getAny(locale, path).let {
        if (it is List<*>) it.filterIsInstance<String>() else {
            throw IllegalArgumentException("Translation $path for $locale is not a list")
        }
    }

    /**
     * Retrieves the translation key located at [path].
     * If the target is a list, a random entry from the list will be returned.
     */
    fun get(locale: Locale, path: String): String = when (val value = getAny(locale, path)) {
        is String -> value
        is List<*> -> value.random() as String
        else -> throw AssertionError("Translation $path for $locale is type ${value::class}")
    }

}
