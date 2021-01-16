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

package com.sandrabot.sandra.managers

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.sandrabot.sandra.entities.Locale
import com.sandrabot.sandra.exceptions.MissingTranslationException
import java.util.*
import kotlin.collections.HashMap

class LanguageManager {

    private val translationMap: Map<Locale, Map<String, Any>>

    init {
        val values = Locale.values()
        val map = HashMap<Locale, Map<String, Any>>(values.size)
        val parser = Parser.default()
        for (it in values) {
            val obj = try {
                val file = LanguageManager::class.java.getResourceAsStream("/languages/${it.identifier}.json")
                        ?: throw IllegalStateException("Language file for ${it.identifier} is missing")
                parser.parse(file) as JsonObject
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to parse language file for ${it.identifier}", e)
            }
            val paths = HashMap<String, Any>()
            loadRecursive("", paths, obj)
            map[it] = paths
        }
        translationMap = Collections.unmodifiableMap(map)
    }

    private fun loadRecursive(root: String, paths: HashMap<String, Any>, obj: JsonObject) {
        for (it in obj.map) {
            val newRoot = if (root.isEmpty()) it.key else "$root.${it.key}"
            when (val value = it.value) {
                is JsonObject -> loadRecursive(newRoot, paths, value)
                is JsonArray<*> -> paths[newRoot] = value.toTypedArray()
                is String -> paths[newRoot] = value
            }
        }
    }

    fun get(locale: Locale, path: String): String {
        // All locales must be loaded or the bot will fail to start
        val map = translationMap[locale] ?: throw AssertionError("Missing translation for $locale")
        val value = map[path] ?: if (locale != Locale.DEFAULT) get(Locale.DEFAULT, path) else {
            throw MissingTranslationException("Missing translation path $path")
        }
        return when (value) {
            is String -> value
            is Array<*> -> value.random() as String
            else -> throw AssertionError("Path $path refers to object $value of type ${value::class}")
        }
    }

}
