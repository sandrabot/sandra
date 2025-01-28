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

@Serializable
data class LastUser(
    val name: String,
    val realName: String,
    val country: String,
    val gender: String,
    val url: String,
    val age: Int,
    val subscriber: Int,
    val playCount: Int,
    val artistCount: Int,
    val albumCount: Int,
    val trackCount: Int,
    val playlists: Int,
    val registeredWhen: Long,
) : ImageHolder()

@Serializable
data class PaginatedResult<T>(
    val page: Int = 1,
    val totalPages: Int = 1,
    val user: String? = null,
    val perPage: Int = -1,
    val total: Int = -1,
    private val results: List<T> = emptyList(),
) : Iterable<T> {
    fun isEmpty() = results.isEmpty()
    override fun iterator(): Iterator<T> = results.iterator()
}

@Serializable
data class Track(
    val name: String,
    val url: String,
    val artist: Artist,
    val duration: Int = -1,
    val playCount: Int = -1,
    val userPlayCount: Int = -1,
    val playedWhen: Long = -1,
    val userLoved: Boolean = false,
    val isNowPlaying: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val album: Album? = null,
    val wiki: Wiki? = null,
) : ImageHolder()

@Serializable
data class Album(
    @JsonNames("#text", "title") val title: String,
    val artist: String? = null,
    val url: String? = null,
) : ImageHolder()

@Serializable
data class Artist(
    @JsonNames("#text", "name") val name: String,
    val url: String? = null,
)

@Serializable
data class CoverImage(
    @JsonNames("#text") val url: String,
    val size: ImageSize = ImageSize.ORIGINAL,
)

@Suppress("unused", "SpellCheckingInspection")
enum class ImageSize {
    SMALL, MEDIUM, LARGE, LARGESQUARE, HUGE, EXTRALARGE, MEGA, ORIGINAL
}

@Serializable
data class Tag(
    val name: String,
    val url: String,
)

@Serializable
data class Wiki(
    val summary: String,
    val content: String,
    val published: String,
)

@Serializable
abstract class ImageHolder {
    @JsonNames("image", "images")
    val images: List<CoverImage> = emptyList()

    fun getImageUrl(size: ImageSize) = images.firstOrNull { it.size == size }?.url?.ifEmpty { null }
}
