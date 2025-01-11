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
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.constants.asEmoji
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.entities.lastfm.ImageSize
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.utils.escape
import com.sandrabot.sandra.utils.sanitize
import com.sandrabot.sandra.utils.tryAverageColor
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.utils.SplitUtil
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused")
class NowPlaying : Command(arguments = "[user]") {

    override val name = "fm"

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

        // retrieve the user's most recently played track
        val track = event.sandra.lastfm.getRecentTracks(username).firstOrNull() ?: run {
            event.sendError(event.getAny("core.lastfm.missing_data", username)).queue()
            return
        }

        val embed = Embed {
            author {
                val key = if (track.isNowPlaying) "now_playing" else "recent"
                name = event.getAny("core.lastfm.$key", user.effectiveName.sanitize())
                url = "https://www.last.fm/user/$username"
                iconUrl = user.effectiveAvatarUrl
            }
            title = track.name
            url = track.url
            description = "**${track.artist.name.escape()}** — *${track.album?.title?.escape()}*"
            thumbnail = track.getImageUrl(ImageSize.EXTRALARGE)?.replace("/300x300", "")
            // add the timestamp only when the track was recently played, rather than now playing
            if (track.playedWhen > 0) timestamp = Instant.ofEpochSecond(track.playedWhen)

            // fetch the track info to display additional context in the footer
            val trackInfo = event.sandra.lastfm.getTrackInfo(track.name, track.artist.name, username)
            // only display the footer if we received a valid response from the api
            if (trackInfo != null) footer(buildString {
                if (trackInfo.userLoved) append(Unicode.MEDIUM_STAR, " ")
                append(event.get("plays", trackInfo.userPlayCount))
                if (trackInfo.duration > 0) append(" • ", trackInfo.duration.milliseconds)
                if (trackInfo.tags.isNotEmpty()) {
                    val firstTags = trackInfo.tags.take(3).joinToString(", ") { it.name.lowercase() }
                    append(" • ", SplitUtil.split(firstTags, 40, SplitUtil.Strategy.onChar(',')).first())
                }
            })

            // download the cover image, if available, and find the average color
            color = (track.tryAverageColor(ImageSize.MEDIUM) ?: event.sandra.settings.color).rgb
        }

        val message = event.sendMessageEmbeds(embed).await()
        // check for "explicit" album cover art, this prevents
        // the embed from being shown in regular channels
        if (message.embeds.isNotEmpty()) {
            // todo feature: customizable reactions
            message.addReaction(Emotes.UPVOTE.asEmoji()).flatMap { message.addReaction(Emotes.DOWNVOTE.asEmoji()) }.queue()
        } else message.editMessage(event.getAny("core.lastfm.explicit", Emotes.NOTICE)).queue()

    }

}
