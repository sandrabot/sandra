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

package com.sandrabot.sandra.managers

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.entities.lastfm.PaginatedResult
import com.sandrabot.sandra.entities.lastfm.RecentTracksSerializer
import com.sandrabot.sandra.entities.lastfm.Track
import com.sandrabot.sandra.entities.lastfm.TrackSerializer
import com.sandrabot.sandra.utils.HTTP_CLIENT
import com.sandrabot.sandra.utils.emptyJsonObject
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory

class LastRequestManager(private val sandra: Sandra) {

    // todo implement request caching
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        decodeEnumsCaseInsensitive = true
        prettyPrint = true
    }

    suspend fun getRecentTracks(
        username: String, page: Int = 1, limit: Int = 10,
    ): PaginatedResult<Track> = submitRequest {
        buildRequest("user.getRecentTracks", mapOf("username" to username, "page" to page, "limit" to limit)).let {
            json.decodeFromJsonElement(RecentTracksSerializer, it)
        }
    }.await()

    suspend fun getTrackInfo(track: String, artist: String, username: String): Track = submitRequest {
        buildRequest("track.getInfo", mapOf("track" to track, "artist" to artist, "username" to username)).let {
            json.decodeFromJsonElement(TrackSerializer, it.jsonObject["track"]!!)
        }
    }.await()

    private suspend fun buildRequest(method: String, params: Map<String, Any>): JsonObject {
        var requestUrl = "$LASTFM_API?method=$method&api_key=${sandra.settings.secrets.lastFmToken}&format=json"
        if (params.isNotEmpty()) {
            requestUrl += params.entries.joinToString(separator = "&", prefix = "&") { (key, value) -> "$key=$value" }
        }
        LOGGER.info("Opening connection: $requestUrl")
        val response = HTTP_CLIENT.get(requestUrl).body<JsonObject>()
        return if (response.isEmpty() || "error" in response) {
            // use the global instance here since we're only encoding, and we don't want it pretty printed
            LOGGER.error("Failed Last.fm request: ${Json.encodeToString(response)}")
            emptyJsonObject()
        } else response
    }

    // todo maybe try making a worker, cuz this seems a lil sus without a queue
    private suspend fun <T : Any> submitRequest(block: suspend () -> T): Deferred<T> =
        withContext(scope.coroutineContext) {
            launch {
                // allow up to 5 requests per second?
                delay(200L)
            }
            async {
                block()
            }
        }

    private companion object {
        private const val LASTFM_API = "https://ws.audioscrobbler.com/2.0/"
        private val LOGGER = LoggerFactory.getLogger(LastRequestManager::class.java)
    }

}
