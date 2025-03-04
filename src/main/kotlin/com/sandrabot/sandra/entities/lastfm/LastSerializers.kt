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

package com.sandrabot.sandra.entities.lastfm

import com.sandrabot.sandra.utils.emptyJsonArray
import com.sandrabot.sandra.utils.emptyJsonObject
import kotlinx.serialization.json.*

object LastUserSerializer : JsonTransformingSerializer<LastUser>(LastUser.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement = buildJsonObject {
        element.jsonObject["user"]?.jsonObject?.forEach { key, value ->
            when (key) {
                "registered" -> put("registeredWhen", value.jsonObject["unixtime"]!!)
                else -> put(key, value)
            }
        }
    }
}

object TrackSerializer : JsonTransformingSerializer<Track>(Track.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement = buildJsonObject {
        element.jsonObject.forEach { key, value ->
            when (key) {
                "userloved" -> put("userLoved", value.jsonPrimitive.int > 0)
                "date" -> put("playedWhen", value.jsonObject["uts"]!!)
                "toptags" -> put("tags", value.jsonObject["tag"]!!)
                "@attr" -> put("isNowPlaying", value.jsonObject["nowplaying"]!!)
                else -> put(key, value)
            }
        }
    }
}

object RecentTracksSerializer : JsonTransformingSerializer<PaginatedResult<Track>>(
    PaginatedResult.serializer(TrackSerializer)
) {
    override fun transformDeserialize(element: JsonElement): JsonElement = buildJsonObject {
        val recentTracks = element.jsonObject["recenttracks"]?.jsonObject ?: emptyJsonObject()
        put("results", recentTracks["track"]?.jsonArray ?: emptyJsonArray())
        recentTracks["@attr"]?.jsonObject?.forEach { key, value -> put(key, value) }
    }
}
