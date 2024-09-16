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

package com.sandrabot.sandra.commands.essential

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.entities.lastfm.ImageSize
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.escape
import com.sandrabot.sandra.utils.sanitize
import com.sandrabot.sandra.utils.tryAverageColor
import dev.minn.jda.ktx.messages.Embed

@Suppress("unused")
class Settings : Command() {

    override suspend fun execute(event: CommandEvent) = Unit

    class LastFm : Command("[@username:text]") {
        // allow the user to just copy and paste the url if they want
        private val profileRegex = Regex("(?:https?://(?:www\\.)?last\\.fm/user/)?([\\w-]{2,15})")

        override suspend fun execute(event: CommandEvent) {
            event.deferReply(ephemeral = true).queue()
            // this is a required argument, it will always be present
            var username = event.arguments.text("username")!!
            // verify the input is a valid last.fm url or plain username
            profileRegex.matchEntire(username)?.let { username = it.groupValues[1] } ?: run {
                event.sendError(event.get("invalid")).queue()
                return
            }
            // ask the api to make sure this account actually exists
            val lastUser = event.sandra.lastfm.getUserInfo(username) ?: run {
                event.sendError(event.get("not_found", username.escape())).queue()
                return
            }
            // update the user's config with the new username
            event.sandra.config[event.user].lastUsername = username
            // reply with an embed that displays their last.fm profile
            event.sendMessageEmbeds(Embed {
                author {
                    name = event.get("connected", event.user.effectiveName.sanitize())
                    url = lastUser.url
                    iconUrl = event.user.effectiveAvatarUrl
                }
                thumbnail = lastUser.getImageUrl(ImageSize.EXTRALARGE)
                color = (lastUser.tryAverageColor(ImageSize.MEDIUM) ?: event.sandra.settings.color).rgb
                description = "${Emotes.SUCCESS} ${event.get("saved", username.escape())}"
                field {
                    name = event.get("created")
                    value = "<t:${lastUser.registeredWhen}:f>, <t:${lastUser.registeredWhen}:R>"
                }
                footer(event.getAny("commands.recent.footer", lastUser.playCount))
            }).queue()
        }
    }

}
