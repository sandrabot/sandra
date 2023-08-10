/*
 * Copyright 2017-2023 Avery Carroll and Logan Devecka
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

package com.sandrabot.sandra.config

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

object ConfigMapTransformer : JsonTransformingSerializer<Map<Long, Configuration>>(
    MapSerializer(Long.serializer(), ConfigSerializer)
) {
    // Convert the map into a list of only values
    override fun transformSerialize(element: JsonElement): JsonElement =
        buildJsonArray { element.jsonObject.map { add(it.value) } }

    // Convert the list back to a map with the id as they key
    override fun transformDeserialize(element: JsonElement): JsonElement =
        buildJsonObject { element.jsonArray.map { put(it.jsonObject["id"].toString(), it.jsonObject) } }
}

object ConfigSerializer : JsonContentPolymorphicSerializer<Configuration>(Configuration::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "cash" in element.jsonObject -> UserConfig.serializer()
        "members" in element.jsonObject -> GuildConfig.serializer()
        "experience" in element.jsonObject -> MemberConfig.serializer()
        else -> ChannelConfig.serializer()
    }
}
