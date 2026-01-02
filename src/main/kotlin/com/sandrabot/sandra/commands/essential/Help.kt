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

import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.Category
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.Thumbnail
import dev.minn.jda.ktx.messages.MessageCreate

@Suppress("unused")
class Help : Command(arguments = "[command]") {

    override suspend fun execute(event: CommandEvent) {
        if (event.options.isEmpty()) event.reply(MessageCreate(useComponentsV2 = true) {
            val invite = if (event.sandra.settings.development) Constants.BETA_INVITE else Constants.DIRECT_INVITE
            val avery = event.sandra.shards.retrieveUserById(Constants.AVERY).await().name
            val logan = event.sandra.shards.retrieveUserById(Constants.LOGAN).await().name
            container {
                text(event.get("title", Emotes.FUN))
                separator(isDivider = false)
                text(event.get("config", Emotes.CONFIG))
                text(event.get("commands", Emotes.COMMANDS))
                text(event.get("invite", Emotes.INVITE, invite))
                text(event.get("support", Emotes.CHAT, Constants.DIRECT_SUPPORT))
                separator()
                text(event.get("creators", Unicode.PINK_HEART, avery, logan))
            }
        }).queue() else {
            val command = event.arguments.command()
            // additionally respond with "not found" if the command can't or shouldn't be listed in /commands
            if (command == null || command.category == Category.CUSTOM || (command.isOwnerOnly && !event.isOwner)) {
                event.replyError(event.get("not_found")).queue()
                return
            }
            event.reply(MessageCreate(useComponentsV2 = true) {
                container {
                    section {
                        accessory = Thumbnail(command.category.emoji.asCustom().imageUrl)
                        text(event.get("extra_help", Constants.DIRECT_SUPPORT))
                        val readablePath = command.path.replace('.', ' ')
                        val commandTitle = event.getAny("commands.commands.title")
                        val description = event.getAny("commands.${command.path}.description")
                        text("## $readablePath ${Unicode.BULLET} ${command.category.name.lowercase()} $commandTitle\n$description")
                        if (command.arguments.isNotEmpty()) {
                            val joined = command.arguments.joinToString(" ") { it.usage }
                            text("### ${Emotes.INFO} ${event.get("usage_title")}\n> **/$readablePath** $joined")
                        }
                    }
                    separator()
                    text(event.get("required_arguments"))
                }
            }).queue()
        }
    }

}
