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

package com.sandrabot.sandra.config

import kotlinx.serialization.json.*

object ConfigSerializer : JsonContentPolymorphicSerializer<Configuration>(Configuration::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "credits" in element.jsonObject -> UserConfig.serializer()
        else -> GuildConfig.serializer()
    }
}

object ConfigTransformer : JsonTransformingSerializer<Configuration>(ConfigSerializer) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        JsonObject(element.jsonObject.filterNot { (k, _) -> k == "type" })
}
