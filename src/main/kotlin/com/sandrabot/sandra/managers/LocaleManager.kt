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

class LocaleManager {

    private val translations: Map<Locale, Map<String, Any>>
    val availableLocales: Set<Locale>

    init {
        val directoryPath = object {}.javaClass.getResource("/translations")?.file
        val translationDir = File(directoryPath ?: throw IllegalStateException("Translation directory is missing"))
        if (!translationDir.isDirectory) throw IllegalStateException("$translationDir is not a directory")
        translations = translationDir.listFiles()?.associate { file ->
            val identifier = file.nameWithoutExtension.replace('_', '-')
            val jsonObject: JsonObject = try {
                val path = file.toRelativeString(translationDir.parentFile)
                val translationJson = getResourceAsText("/$path")
                    ?: throw IllegalStateException("File for $identifier is missing, expected at $path")
                Json.decodeFromString(translationJson)
            } catch (t: Throwable) {
                throw IllegalStateException("Failed to parse translation file for $identifier", t)
            }
            val pathMap = mutableMapOf<String, Any>()
            loadRecursive("", pathMap, jsonObject)
            Locale.forLanguageTag(identifier) to pathMap
        } ?: throw IllegalStateException("Failed to initialize translations")
        availableLocales = translations.keys
    }

    private fun loadRecursive(root: String, pathMap: MutableMap<String, Any>, jsonObject: JsonObject) {
        for (entry in jsonObject.entries) {
            val newRoot = if (root.isEmpty()) entry.key else "$root.${entry.key}"
            when (val value = entry.value) {
                is JsonObject -> loadRecursive(newRoot, pathMap, value)
                is JsonPrimitive -> pathMap[newRoot] = value.content
                is JsonArray -> pathMap[newRoot] = value.jsonArray.map { it.jsonPrimitive.content }.toList()
            }
        }
    }

    private fun getAny(locale: Locale, path: String): Any {
        val translation = translations[if (locale in translations) locale else Locale.US]
            ?: throw AssertionError("Missing translation map for $locale and ${Locale.US}")
        return translation[path] ?: throw MissingTranslationException("Missing translation path $path for $locale")
    }

    fun getList(locale: Locale, path: String): List<String> = getAny(locale, path).let {
        if (it is List<*>) it.filterIsInstance<String>() else {
            throw IllegalArgumentException("Translation $path for $locale is not a list")
        }
    }

    fun get(locale: Locale, path: String): String = when (val value = getAny(locale, path)) {
        is String -> value
        is List<*> -> value.random() as String
        else -> throw AssertionError("Translation $path for $locale is type ${value::class}")
    }

}
