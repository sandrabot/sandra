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

import com.sandrabot.sandra.constants.Colors
import com.sandrabot.sandra.constants.Emotes
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
import javax.imageio.ImageIO

@Suppress("unused")
class Recent : Command(arguments = "[user]") {

    override suspend fun execute(event: CommandEvent) {

        val user = event.arguments.user() ?: event.user
        // make sure the user has entered their last.fm username
        val username = event.sandra.config[user].lastUsername ?: run {
            val key = if (user == event.user) "missing_username" else "missing_other"
            event.replyEmoji(Emotes.LASTFM, event.getAny("core.lastfm.$key", user)).asEphemeral().queue()
            return
        }

        // acknowledge the interaction while we wait for additional requests
        event.deferReply().queue()

        // retrieve the user's most recently played tracks
        val recentTracks = event.sandra.lastfm.getRecentTracks(username)
        // make sure we received a valid response from the api
        if (recentTracks.isEmpty()) {
            event.sendError(event.getAny("core.lastfm.missing_data", username)).queue()
            return
        }

        val embed = Embed {
            author {
                name = event.getAny("core.lastfm.recent", user.effectiveName.sanitize())
                url = "https://www.last.fm/user/$username"
                iconUrl = user.effectiveAvatarUrl
            }
            thumbnail = recentTracks.first().getImageUrl(ImageSize.EXTRALARGE)?.replace("/300x300", "")
            // sometimes the api returns a total of 11 tracks when the user is listening to something
            description = recentTracks.take(10).mapIndexed { index, track ->
                val timePlayed = if (track.playedWhen > 0) {
                    // determine if the track was played today, then format accordingly
                    val playedToday = System.currentTimeMillis() / 1000 - track.playedWhen < 86_400
                    "<t:${track.playedWhen}:${if (playedToday) "t" else "f"}>"
                } else event.get("now_playing")
                event.get(
                    "entry", index + 1, track.name.escape(), track.url,
                    track.artist.name.escape(), timePlayed, track.album?.title?.escape()
                )
            }.joinToString("\n")
            footer(event.get("footer", recentTracks.total))
            // download the cover image, if available, and find the average color
            recentTracks.first().getImageUrl(ImageSize.MEDIUM)?.let { url ->
                color = try {
                    val stream = HTTP_CLIENT.get(url).body<InputStream>()
                    findTrueAverageColor(ImageIO.read(stream))
                } catch (_: Exception) {
                    // unable to decode the image, just use the default color instead
                    Colors.WELL_READ
                }.rgb
            }
        }

        event.sendMessageEmbeds(embed).queue()

    }

}
