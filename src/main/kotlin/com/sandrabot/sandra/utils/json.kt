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

package com.sandrabot.sandra.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
fun Map<*, *>.toJson(json: Json = Json) = json.encodeToString(toJsonObject())

fun Map<*, *>.toJsonObject(): JsonObject = JsonObject(map {
    it.key.toString() to it.value.toJsonElement()
}.toMap())

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> this.toJsonObject()
    is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
    is Array<*> -> JsonArray(this.map { it.toJsonElement() })
    else -> JsonPrimitive(this.toString()) // Or throw some "unsupported" exception?
}

fun JsonObject.obj(name: String): JsonObject? = getEscaped(name)?.jsonObject
fun JsonObject.array(name: String): JsonArray? = getEscaped(name)?.jsonArray
fun JsonObject.int(name: String): Int? = getEscaped(name)?.jsonPrimitive?.intOrNull
fun JsonObject.long(name: String): Long? = getEscaped(name)?.jsonPrimitive?.longOrNull
fun JsonObject.boolean(name: String): Boolean? = getEscaped(name)?.jsonPrimitive?.boolean
fun JsonObject.string(name: String): String? = getEscaped(name)?.jsonPrimitive?.contentOrNull

fun JsonObject.getEscaped(name: String): JsonElement? = get(name)?.escapeJsonNull()
fun JsonElement.escapeJsonNull(): JsonElement? = if (this is JsonNull) null else this
