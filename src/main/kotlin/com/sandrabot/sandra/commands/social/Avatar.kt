/*
 * Copyright 2026 Avery Carroll, Logan Devecka, and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandrabot.sandra.commands.social

import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import dev.minn.jda.ktx.messages.MessageCreate

@Suppress("unused")
class Avatar : Command(arguments = "[user]") {

    override suspend fun execute(event: CommandEvent) {

        // unless the user argument was supplied, display the current user's avatar
        val targetUser = event.arguments.user() ?: event.user
        // respond to the interaction with the selected user's avatar
        event.reply(MessageCreate(useComponentsV2 = true) {
            // use the largest possible resolution for the avatar image
            val avatarUrl = targetUser.effectiveAvatarUrl.let { if ('?' in it) "$it&" else "$it?" } + "size=2048"
            container {
                text(event.get("title", targetUser))
                mediaGallery { item(avatarUrl) }
            }
            // add a button with a link to quickly open the image url
            actionRow { linkButton(avatarUrl, event.get("button")) }
            // to prevent mentioning the current user, disable all mentions in the response
        }).setAllowedMentions(emptyList()).asEphemeral().queue()

    }

}
