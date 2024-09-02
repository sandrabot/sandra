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

package com.sandrabot.sandra.utils

import kotlinx.serialization.json.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

fun Map<*, *>.toJsonObject() = buildJsonObject {
    forEach { (key, value) -> put(key.toString(), value.toJsonElement()) }
}

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Map<*, *> -> toJsonObject()
    is Iterable<*> -> toJsonArray()
    is Array<*> -> toJsonArray()
    else -> JsonPrimitive(toString())
}

fun Iterable<*>.toJsonArray() = JsonArray(map { it.toJsonElement() })
fun Array<*>.toJsonArray() = JsonArray(map { it.toJsonElement() })

fun JsonObject.flatten(root: String = "", map: MutableMap<String, Any> = mutableMapOf()): Map<String, Any> =
    entries.forEach { (key, value) ->
        val name = if (root.isNotEmpty()) "$root.$key" else key
        when (value) {
            is JsonObject -> value.flatten(name, map)
            is JsonArray -> map[name] = value.flatten()
            is JsonPrimitive -> map[name] = value.content
        }
    }.let { map }

fun JsonArray.flatten() = map {
    when (it) {
        is JsonObject -> it.flatten()
        is JsonArray -> it.toList()
        is JsonPrimitive -> it.content
    }
}

fun JsonElement.asInt() = jsonPrimitive.content.toInt()
