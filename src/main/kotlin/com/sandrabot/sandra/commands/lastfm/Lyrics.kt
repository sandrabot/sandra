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
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.entities.Paginator
import com.sandrabot.sandra.entities.lastfm.ImageSize
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.utils.escape
import com.sandrabot.sandra.utils.sanitize
import com.sandrabot.sandra.utils.tryAverageColor
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.utils.SplitUtil

@Suppress("unused")
class Lyrics : Command("[user]") {

    override suspend fun execute(event: CommandEvent) {

        val user = event.arguments.user() ?: event.user
        // verify the user has entered their last.fm username
        val username = event.sandra.config[user].lastUsername ?: run {
            val key = if (user == event.user) "missing_username" else "missing_other"
            event.replyEmoji(Emotes.LASTFM, event.getAny("core.lastfm.$key", user)).asEphemeral().queue()
            return
        }

        // acknowledge the interaction while we make additional requests
        event.deferReply().queue()

        // retrieve the user's most recently played track
        val track = event.sandra.lastfm.getRecentTracks(username).firstOrNull() ?: run {
            event.sendError(event.getAny("core.lastfm.missing_data", username)).queue()
            return
        }
        // todo decide if this command should only be available to current listeners
        if (!track.isNowPlaying) {
            event.sendInfo(event.get("not_playing")).queue()
            return
        }

        // todo search and retrieve lyrics using an api
        val lyrics = """
            Lorem ipsum odor amet, consectetuer adipiscing elit.
            Porttitor urna donec sem tempor porta tincidunt.
            Dignissim tincidunt lobortis convallis tempor adipiscing pretium scelerisque consequat.
            Viverra platea arcu amet cubilia elementum etiam quis sollicitudin.
            Dis diam justo congue pharetra enim pellentesque lacus viverra.

            Platea montes vivamus sit diam netus sodales.
            Duis vitae egestas condimentum ad suscipit vehicula neque!
            Metus justo nam tellus ultrices vulputate auctor, consectetur ultricies?
            Fringilla leo arcu gravida montes finibus rhoncus cras tellus.
            Molestie orci ligula augue elementum risus facilisis lacinia phasellus quam.

            Habitant conubia elit vulputate blandit justo bibendum ex.
            Mauris natoque ridiculus sodales nec augue phasellus ante.
            Finibus aliquam cras tristique semper conubia.
            Ex nunc morbi a massa sagittis.

            Pulvinar ullamcorper neque dictum donec sed mi.
            Porta blandit curae aliquet tempor mi laoreet.
            Efficitur nunc lobortis aliquam rhoncus hac odio.
            Inceptos convallis est habitant erat integer lacus.
        """.trimIndent()

        Paginator(event).paginate(SplitUtil.split(lyrics, 800, SplitUtil.Strategy.NEWLINE).map { page ->
            Embed {
                author {
                    name = event.getAny("core.lastfm.now_playing", user.effectiveName.sanitize())
                    url = "https://www.last.fm/user/$username"
                    iconUrl = user.effectiveAvatarUrl
                }
                title = track.name
                url = track.url
                description = "**${track.artist.name.escape()}** • *${track.album?.title?.escape()}*\n\n${page.trim()}"
                thumbnail = track.getImageUrl(ImageSize.EXTRALARGE)?.replace("/300x300", "")
                color = (track.tryAverageColor(ImageSize.MEDIUM) ?: event.sandra.settings.color).rgb
                footer(event.get("powered_by"))
            }
        })

    }

}
