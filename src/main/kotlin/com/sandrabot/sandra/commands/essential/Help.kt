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
import com.sandrabot.sandra.constants.asEmoji
import com.sandrabot.sandra.entities.Category
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import dev.minn.jda.ktx.coroutines.await

@Suppress("unused")
class Help : Command(arguments = "[command]") {

    override suspend fun execute(event: CommandEvent) {

        if ("command" in event.arguments) {
            val command = event.arguments.command()
            // additionally respond with "not found" if the command can't or shouldn't be listed in /commands
            if (command == null || (command.isOwnerOnly && !event.isOwner) || command.category == Category.CUSTOM) {
                event.replyError(event.get("not_found")).setEphemeral(true).queue()
                return
            }

            // Begin putting the embed together, starting with the command path and category
            val readablePath = command.path.replace('.', ' ')
            val wordCommands = event.getAny("commands.commands.command_title")
            val author = "$readablePath • ${command.category.name.lowercase()} $wordCommands"
            // The command's category emote is used as the author image
            val emojiUrl = command.category.emote.asEmoji().asCustom().imageUrl
            val embed = event.embed().setAuthor(author).setThumbnail(emojiUrl)
            embed.setTitle(event.get("extra_help"), Constants.DIRECT_SUPPORT)
            // Retrieve the translation for the command's description, this time we need to not use the root
            val descriptionValue = "> ${event.getAny("commands.${command.path}.description")}"
            embed.addField("${Emotes.PROMPT} ${event.get("description_title")}", descriptionValue, false)
            // Display a field describing the command usage if there's any arguments
            if (command.arguments.isNotEmpty()) {
                // Combine all the arguments into a string to be displayed
                val joined = command.arguments.joinToString(" ") { it.usage }
                val usageValue = "> **/$readablePath** $joined"
                embed.addField("${Emotes.INFO} ${event.get("usage_title")}", usageValue, false)
                // Set the footer as well for context about arguments
                embed.setFooter(event.get("required_arguments"))
            }
            event.replyEmbeds(embed.build()).setEphemeral(true).queue()
            return
        }

        event.deferReply(ephemeral = true).await()
        // If no arguments were supplied, just show information about the bot
        val lang = event.localeContext.withRoot("commands.help.info_embed")
        val embed = event.embed().setTitle(lang["title"])
        embed.setThumbnail(event.selfUser.effectiveAvatarUrl)
        embed.addField(lang["configure", Emotes.CONFIG], lang["configure_content"], false)
        embed.addField(lang["commands", Emotes.COMMANDS], lang["commands_content"], false)

        val invite = if (event.sandra.settings.development) Constants.BETA_INVITE else Constants.DIRECT_INVITE
        embed.addField(lang["invite", Emotes.INVITE], lang["invite_content", invite], false)
        embed.addField(lang["support", Emotes.CHAT], lang["support_content", Constants.DIRECT_SUPPORT], false)

        val devs = Constants.DEVELOPERS.map { event.sandra.shards.retrieveUserById(it).await().name }.toTypedArray()
        embed.setFooter(lang.get("built", Unicode.HEAVY_BLACK_HEART, *devs))

        event.sendMessageEmbeds(embed.build()).setEphemeral(true).queue()

    }

}
