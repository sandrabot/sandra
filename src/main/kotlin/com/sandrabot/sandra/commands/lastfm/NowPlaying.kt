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

package com.sandrabot.sandra.commands.lastfm

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.asEmoji
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.entities.lastfm.ImageSize
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.utils.HTTP_CLIENT
import com.sandrabot.sandra.utils.escape
import com.sandrabot.sandra.utils.findTrueAverageColor
import com.sandrabot.sandra.utils.sanitize
import dev.minn.jda.ktx.messages.Embed
import io.ktor.client.call.*
import io.ktor.client.request.*
import java.io.InputStream
import java.time.Instant
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused")
class NowPlaying : Command(arguments = "[user]") {

    override val name = "fm"

    override suspend fun execute(event: CommandEvent) {

        val user = event.arguments.user() ?: event.user
        // ensure that the user has entered their last.fm username
        val username = event.sandra.config[user].lastUsername ?: run {
            val key = if (user == event.user) "missing_username" else "missing_other"
            event.replyInfo(event.getAny("core.lastfm.$key", user)).asEphemeral().queue()
            return
        }

        // acknowledge the interaction while we wait for additional requests
        event.deferReply().queue()

        // retrieve the user's most recently played track
        val track = event.sandra.lastfm.getRecentTracks(username).firstOrNull() ?: run {
            event.sendError(event.getAny("core.lastfm.missing_data", username)).queue()
            return
        }

        val embed = Embed {
            author {
                val context = if (track.isNowPlaying) "now_playing" else "recent"
                name = event.get(context, user.effectiveName.sanitize())
                url = "https://www.last.fm/user/$username"
                iconUrl = user.effectiveAvatarUrl
            }
            title = track.name
            url = track.url
            description = "**${track.artist.name.escape()}** • *${track.album?.title?.escape()}*"
            thumbnail = track.getImageUrl(ImageSize.EXTRALARGE)?.replace("/300x300", "")
            // add the timestamp only when the track was recently played, rather than now playing
            if (track.playedWhen > 0) timestamp = Instant.ofEpochSecond(track.playedWhen)

            // fetch the track info to display additional context in the footer
            val trackInfo = event.sandra.lastfm.getTrackInfo(track.name, track.artist.name, username)
            // only display the footer if we received a valid response from the api
            if (trackInfo != null) footer(buildString {
                append(event.get("plays", trackInfo.userPlayCount))
                if (trackInfo.duration > 0) append(" • ", trackInfo.duration.milliseconds)
                if (trackInfo.tags.isNotEmpty()) {
                    append(" • ", trackInfo.tags.take(3).joinToString(", ") { it.name.lowercase() })
                }
            })

            // download the cover image, if available, and find the average color
            track.getImageUrl(ImageSize.MEDIUM)?.let {
                val stream = HTTP_CLIENT.get(it).body<InputStream>()
                color = findTrueAverageColor(ImageIO.read(stream)).rgb
            }
        }

        event.sendMessageEmbeds(embed).flatMap { message ->
            // todo feature: customizable reactions
            message.addReaction(Emotes.UPVOTE.asEmoji()).flatMap { message.addReaction(Emotes.DOWNVOTE.asEmoji()) }
        }.queue()

    }

}
