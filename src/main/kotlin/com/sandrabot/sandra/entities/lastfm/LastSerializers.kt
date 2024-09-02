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

import com.sandrabot.sandra.utils.asInt
import kotlinx.serialization.json.*

object TrackSerializer : JsonTransformingSerializer<Track>(Track.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement = buildJsonObject {
        element.jsonObject.forEach { key, value ->
            when (key) {
                "duration" -> put("duration", value.asInt())
                "playcount" -> put("playCount", value.asInt())
                "userplaycount" -> put("userPlayCount", value.asInt())
                "userloved" -> put("userLoved", value.asInt())
                "toptags" -> put("tags", value.jsonObject["tag"]!!.jsonArray)
                "@attr" -> put("isNowPlaying", value.jsonObject["nowplaying"]!!.jsonPrimitive.boolean)
                "image" -> put("images", value)
                else -> put(key, value)
            }
        }
    }
}

object RecentTracksSerializer : JsonTransformingSerializer<PaginatedResult<Track>>(
    PaginatedResult.serializer(TrackSerializer)
) {
    override fun transformDeserialize(element: JsonElement): JsonElement = buildJsonObject {
        val recentTracks = element.jsonObject["recenttracks"]!!.jsonObject
        put("results", recentTracks["track"]!!.jsonArray)
        recentTracks["@attr"]!!.jsonObject.forEach { key, value ->
            when (key) {
                "user" -> put("user", value.jsonPrimitive.content)
                else -> put(key, value.asInt())
            }
        }
    }
}
