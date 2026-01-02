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

package com.sandrabot.sandra.commands.social

import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import dev.minn.jda.ktx.messages.MessageCreate

@Suppress("unused")
class Avatar : Command(arguments = "[user]") {

    override suspend fun execute(event: CommandEvent) {

        val user = event.arguments.user() ?: event.user
        val url = user.effectiveAvatarUrl + "?size=2048" // largest possible resolution
        event.reply(MessageCreate(useComponentsV2 = true) {
            container {
                mediaGallery { item(url) }
                text("# @${user.name}")
                separator(isDivider = false)
                actionRow { linkButton(url, event.get("button")) }
            }
        }).asEphemeral().queue()

    }

}
