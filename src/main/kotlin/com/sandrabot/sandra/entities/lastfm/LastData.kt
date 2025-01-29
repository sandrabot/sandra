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

@file:OptIn(ExperimentalSerializationApi::class)

package com.sandrabot.sandra.entities.lastfm

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// This file contains a collection of data objects used to deserialize Last.fm API responses.
// Refer to the Last.fm API documentation for more information. https://www.last.fm/api

/**
 * A [Last.fm user](https://www.last.fm/api/show/user.getInfo) object.
 */
@Serializable
data class LastUser(
    /**
     * The user's Last.fm username
     */
    val name: String,

    /**
     * The user's real name, if provided
     */
    @JsonNames("realname") val realName: String,

    /**
     * The user's country, if provided
     */
    val country: String,

    /**
     * The user's gender, if provided
     */
    val gender: String,

    /**
     * A link to the user's Last.fm profile
     */
    val url: String,

    /**
     * The user's age, if provided
     */
    val age: Int,

    /**
     * The user's subscriber status
     */
    val subscriber: Int,

    /**
     * The total number of scrobbles the user has made
     */
    @JsonNames("playcount") val playCount: Int,

    /**
     * The number of unique artists the user has scrobbled
     */
    @JsonNames("artist_count") val artistCount: Int,

    /**
     * The number of unique albums the user has scrobbled
     */
    @JsonNames("album_count") val albumCount: Int,

    /**
     * The number of unique tracks the user has scrobbled
     */
    @JsonNames("track_count") val trackCount: Int,

    /**
     * The number of playlists the user has created
     */
    val playlists: Int,

    /**
     * The date the user registered their Last.fm account
     */
    val registeredWhen: Long,
) : ImageHolder()

/**
 * A [Last.fm track](https://www.last.fm/api/show/track.getInfo) object.
 */
@Serializable
data class Track(
    /**
     * The track name
     */
    val name: String,

    /**
     * A link to the track information on Last.fm
     */
    val url: String,

    /**
     * The artist that created the track
     */
    val artist: Artist,

    /**
     * The duration of the track in milliseconds
     */
    val duration: Int = -1,

    /**
     * The number of times this track has been scrobbled globally
     */
    @JsonNames("playcount") val playCount: Int = -1,

    /**
     * The number of times the user has scrobbled this track
     */
    @JsonNames("userplaycount") val userPlayCount: Int = -1,

    /**
     * The last time the user scrobbled this track
     */
    val playedWhen: Long = -1,

    /**
     * Whether the user has loved the track or not
     */
    val userLoved: Boolean = false,

    /**
     * Whether the track is currently playing or not
     */
    val isNowPlaying: Boolean = false,

    /**
     * A list of tags associated with the track
     */
    val tags: List<Tag> = emptyList(),

    /**
     * The album that this track belongs to
     */
    val album: Album? = null,

    /**
     * Additional information about this track
     */
    val wiki: Wiki? = null,
) : ImageHolder()

/**
 * A [Last.fm paginated result](https://www.last.fm/api/show/user.getRecentTracks) of recent tracks.
 */
@Serializable
data class PaginatedResult<T>(
    /**
     * The current page of results
     */
    val page: Int = 1,

    /**
     * The total number of pages containing results
     */
    val totalPages: Int = 1,

    /**
     * The number of results per page
     */
    val perPage: Int = -1,

    /**
     * The total number of results
     */
    val total: Int = -1,

    /**
     * The user that the results pertain to, if provided
     */
    val user: String? = null,

    /**
     * The results of the current page
     */
    private val results: List<T> = emptyList(),
) : Iterable<T> {
    /**
     * Returns `true` if no results are present
     */
    fun isEmpty() = results.isEmpty()

    override fun iterator(): Iterator<T> = results.iterator()
}

// Track member objects

/**
 * Represents a [Track]'s album information.
 */
@Serializable
data class Album(
    /**
     * The title of the album
     */
    @JsonNames("#text") val title: String,

    /**
     * The artist that created the album
     */
    val artist: String? = null,

    /**
     * A link to the album information on Last.fm
     */
    val url: String? = null,
) : ImageHolder()

/**
 * Represents a [Track]'s artist information.
 */
@Serializable
data class Artist(
    /**
     * The name of the artist
     */
    @JsonNames("#text") val name: String,

    /**
     * A link to the artist information on Last.fm
     */
    val url: String? = null,
)

/**
 * Represents a [Track]'s tag information.
 */
@Serializable
data class Tag(
    /**
     * The name of the tag
     */
    val name: String,

    /**
     * A link to the tag information on Last.fm
     */
    val url: String,
)

/**
 * Represents a [Track]'s additional information
 */
@Serializable
data class Wiki(
    /**
     * A summary of the information in [content]
     */
    val summary: String,

    /**
     * Additional information provided by the community
     */
    val content: String,

    /**
     * When this wiki was published
     */
    val published: String,
)

// Image holder objects

/**
 * Allows certain objects to hold a list of images.
 *
 * @see CoverImage
 */
@Serializable
abstract class ImageHolder {
    @JsonNames("image")
    val images: List<CoverImage> = emptyList()

    /**
     * Retrieves the URL of the image in the specified size, or `null` if it doesn't exist.
     */
    fun getImageUrl(size: ImageSize) = images.firstOrNull { it.size == size }?.url?.ifEmpty { null }
}

/**
 * Associates the given [url] with the appropriate [ImageSize].
 */
@Serializable
data class CoverImage(
    @JsonNames("#text") val url: String,
    val size: ImageSize = ImageSize.ORIGINAL,
)

/**
 * A list of possible image sizes that the Last.fm API may return.
 */
@Suppress("unused", "SpellCheckingInspection")
enum class ImageSize {
    SMALL, MEDIUM, LARGE, LARGESQUARE, HUGE, EXTRALARGE, MEGA, ORIGINAL
}
