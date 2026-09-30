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

package com.sandrabot.sandra.commands.essential

import com.sandrabot.sandra.constants.Category
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emojis
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.utils.getCommandDescription
import com.sandrabot.sandra.utils.getCommandFullName
import com.sandrabot.sandra.utils.getCommandUsage
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.Thumbnail
import dev.minn.jda.ktx.messages.MessageCreate

@Suppress("unused")
class Help : Command(arguments = "[command]") {

    override suspend fun execute(event: CommandEvent) {

        // when no arguments are supplied, respond with help info
        if (event.options.isEmpty()) {

            // reply with some general information about the bot
            event.reply(MessageCreate(useComponentsV2 = true) {
                // determine which application invite link to use for quickly adding the bot
                val invite = if (event.sandra.settings.development) Constants.BETA_INVITE else Constants.DIRECT_INVITE
                // retrieve the usernames for the developer acknowledgement in the footer
                val avery = event.sandra.shards.retrieveUserById(Constants.AVERY).await().name
                val logan = event.sandra.shards.retrieveUserById(Constants.LOGAN).await().name
                container {
                    text(event.get("title", Emojis.FUN))
                    separator(isDivider = false)
                    text(event.get("config", Emojis.CONFIG))
                    text(event.get("commands", Emojis.TERMINAL))
                    text(event.get("invite", Emojis.INVITE, invite))
                    text(event.get("support", Emojis.CHAT, Constants.DIRECT_SUPPORT))
                    separator()
                    text(event.get("creators", Unicode.PINK_HEART, avery, logan))
                }
            }).queue() // not ephemeral, can be used to inform other users

        } else {

            // an argument was submitted, resolve the command
            val command = event.arguments.command()
            // respond with "not found" if the command doesn't exist, or if it should be hidden
            if (command == null || command.category == Category.CUSTOM || (command.isOwnerOnly && !event.isOwner)) {
                event.replyNotice(event.get("not_found")).asEphemeral().queue()
                return
            }

            // create and send a message with all the relevant information for this command
            event.reply(MessageCreate(useComponentsV2 = true) {
                container {
                    section {
                        // show the command category emoji as the thumbnail image
                        accessory = Thumbnail(command.category.emoji.asCustom().imageUrl)
                        val fullName = event.localeContext.getCommandFullName(command)
                        val description = event.localeContext.getCommandDescription(command)
                        text("# /$fullName ${Unicode.BULLET} ${event.get("name")}\n> $description")
                        // list all the command arguments, if applicable
                        if (command.arguments.isNotEmpty()) {
                            val usage = event.localeContext.getCommandUsage(command)
                            text("### ${Emojis.PROMPT} ${event.get("usage")}\n`/$fullName $usage`")
                        }
                        // list all the available subcommands, if applicable
                        if (command.subcommands.isNotEmpty()) {
                            val otherCommands = buildString {
                                command.subcommands.forEach { command ->
                                    val fullName = event.localeContext.getCommandFullName(command)
                                    val description = event.localeContext.getCommandDescription(command)
                                    append("`/", fullName, "` - ", description, "\n")
                                }
                            }
                            text("### ${Emojis.TERMINAL} ${event.get("subcommands")}\n>>> $otherCommands")
                        }
                    }
                    text(event.get("extra_help", Constants.DIRECT_SUPPORT))
                    separator()
                    text(event.get("required_arguments"))
                }
            }).queue() // not ephemeral, can be used to inform other users

        }

    }

}
