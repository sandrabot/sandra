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

package com.sandrabot.sandra.managers

import com.sandrabot.sandra.entities.Locale
import com.sandrabot.sandra.exceptions.MissingTranslationException
import com.sandrabot.sandra.utils.getResourceAsText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
class LocaleManager {

    private val translationMap = mutableMapOf<Locale, Map<String, Any>>()

    init {
        for (it in Locale.values()) {
            val jsonObject = try {
                val text = getResourceAsText("/translations/${it.identifier}.json")
                    ?: throw IllegalStateException("Translation file for ${it.identifier} is missing")
                Json.decodeFromString<JsonObject>(text)
            } catch (t: Throwable) {
                throw IllegalArgumentException("Failed to parse translation file for ${it.identifier}", t)
            }
            val pathMap = mutableMapOf<String, Any>()
            loadRecursive("", pathMap, jsonObject)
            translationMap[it] = pathMap
        }
    }

    private fun loadRecursive(root: String, paths: MutableMap<String, Any>, obj: JsonObject) {
        for (it in obj.entries) {
            val newRoot = if (root.isEmpty()) it.key else "$root.${it.key}"
            when (val value = it.value) {
                is JsonObject -> loadRecursive(newRoot, paths, value)
                is JsonArray -> paths[newRoot] = value.jsonArray.map { it.jsonPrimitive.content }.toTypedArray()
                is JsonPrimitive -> paths[newRoot] = value.content
            }
        }
    }

    private fun getAny(locale: Locale, path: String): Any {
        // All locales must be loaded or the bot will fail to start
        val map = translationMap[locale] ?: throw AssertionError("Missing translation for $locale")
        return map[path] ?: if (locale != Locale.DEFAULT) get(Locale.DEFAULT, path) else {
            throw MissingTranslationException("Missing translation path $path")
        }
    }

    fun getList(locale: Locale, path: String): List<String> = getAny(locale, path).let {
        if (it is Array<*>) it.filterIsInstance<String>() else
            throw IllegalArgumentException("Path $path of value $it is not an array: ${it::class}")
    }

    fun get(locale: Locale, path: String): String = when (val value = getAny(locale, path)) {
        is String -> value
        is Array<*> -> value.random() as String
        else -> throw AssertionError("Path $path refers to object $value of type ${value::class}")
    }

}
