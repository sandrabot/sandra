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
import com.sandrabot.sandra.entities.SimpleRateLimiter
import com.sandrabot.sandra.entities.lastfm.*
import com.sandrabot.sandra.utils.HTTP_CLIENT
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class LastRequestManager(private val sandra: Sandra) {

    // limit last.fm api calls across the application to 5 requests per second
    private val rateLimiter = SimpleRateLimiter(5.0)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cache: ExpiringMap<Int, JsonObject> =
        ExpiringMap.builder().expirationPolicy(ExpirationPolicy.CREATED).expiration(30, TimeUnit.SECONDS).build()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        decodeEnumsCaseInsensitive = true
    }

    suspend fun getTrackInfo(track: String, artist: String, username: String): Track? = buildRequest(
        "track.getInfo", "track" to track, "artist" to artist, "username" to username
    )?.let { response -> json.decodeFromJsonElement(TrackSerializer, response.jsonObject["track"]!!) }

    suspend fun getUserInfo(username: String): LastUser? = buildRequest(
        "user.getInfo", "username" to username
    )?.let { response -> json.decodeFromJsonElement(LastUserSerializer, response) }

    suspend fun getRecentTracks(
        username: String, page: Int = 1, limit: Int = 10,
    ): PaginatedResult<Track>? = buildRequest(
        "user.getRecentTracks", "username" to username, "page" to page, "limit" to limit
    )?.let { response -> json.decodeFromJsonElement(RecentTracksSerializer, response) }

    private suspend fun buildRequest(method: String, vararg params: Pair<String, Any>): JsonObject? {
        var requestUrl = "$LASTFM_API?method=$method&api_key=${sandra.settings.secrets.lastFmToken}&format=json"
        if (params.isNotEmpty()) requestUrl += params.joinToString(separator = "&", prefix = "&") { (key, value) ->
            "$key=${value.toString().encodeURLParameter()}"
        }
        val requestHash = requestUrl.hashCode()
        // check the cache to see if this request was made recently
        cache[requestHash]?.let { hit ->
            LOGGER.debug("Cache hit for request: $requestUrl")
            return hit
        }
        LOGGER.debug("Opening connection: $requestUrl")
        val response = withContext(scope.coroutineContext) {
            rateLimiter.acquire()
            HTTP_CLIENT.get(requestUrl).body<JsonObject>()
        }
        return if (response.isEmpty() || "error" in response) {
            LOGGER.error("Failed Last.fm request: ${json.encodeToString(response)}")
            null
        } else response.also { cache[requestHash] = it }
    }

    private companion object {
        private const val LASTFM_API = "https://ws.audioscrobbler.com/2.0/"
        private val LOGGER = LoggerFactory.getLogger(LastRequestManager::class.java)
    }

}
