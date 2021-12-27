/*
 * Copyright 2017-2022 Avery Carroll and Logan Devecka
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
import com.sandrabot.sandra.utils.asEmoteUrl
import com.sandrabot.sandra.utils.await

@Suppress("unused")
class Help : Command(name = "help", arguments = "[command]") {

    override suspend fun execute(event: CommandEvent) {

        if (event.argumentString.isNotEmpty()) {

            val command = event.arguments.command() ?: run {
                // The command couldn't be found with the given path
                event.replyError(event.translate("not_found")).queue()
                return
            }

            if (command.ownerOnly || command.category == Category.CUSTOM) {
                event.replyError(event.translate("not_found")).queue()
                return
            }

            // Begin putting the embed together, starting with the command path and category
            val commandPath = command.path.replace('/', ' ')
            val author = "$commandPath • ${command.category.name.lowercase()}"
            // The command's category emote is used as the author image
            val embed = event.embed.setAuthor(author, null, command.category.emote.asEmoteUrl())
            embed.setTitle(event.translate("extra_help"), Constants.DIRECT_SUPPORT)
            // Retrieve the translation for the command's description, this time we need to not use the root
            val descriptionPath = "commands.${command.path.replace('/', '.')}.description"
            val descriptionValue = "> ${event.translate(descriptionPath, false)}"
            embed.addField("${Emotes.PROMPT} ${event.translate("description_title")}", descriptionValue, false)
            // Display a field describing the command usage if there's any arguments
            if (command.arguments.isNotEmpty()) {
                // Combine all the arguments into a string to be displayed
                val joined = command.arguments.joinToString(" ") { it.usage }
                val usageValue = "> **/$commandPath** $joined"
                embed.addField("${Emotes.INFO} ${event.translate("usage_title")}", usageValue, false)
                // Set the footer as well for context about arguments
                embed.setFooter(event.translate("required_arguments"))
            }
            event.reply(embed.build()).queue()
            return
        }

        event.deferReply().await()
        // If no arguments were supplied, just show information about the bot
        val lang = event.localeContext.withRoot("commands.help.info_embed")
        val embed = event.embed.setTitle(lang.translate("title"), Constants.DIRECT_SUPPORT)
        embed.setThumbnail(event.selfUser.effectiveAvatarUrl)

        val configureContent = lang.translate("configure_content")
        val commandsContent = lang.translate("commands_content")
        val inviteContent = lang.translate("invite_content", Constants.DIRECT_INVITE)
        val supportContent = lang.translate("support_content", Constants.DIRECT_SUPPORT)
        embed.addField(lang.translate("configure", Emotes.CONFIG), configureContent, false)
        embed.addField(lang.translate("commands", Emotes.COMMANDS), commandsContent, false)
        embed.addField(lang.translate("invite", Emotes.NOTIFY), inviteContent, false)
        embed.addField(lang.translate("support", Emotes.BUBBLES), supportContent, false)

        val devs = Constants.DEVELOPERS.mapNotNull { event.sandra.retrieveUser(it)?.asTag }.joinToString(" and ")
        embed.setFooter("Built with ${Unicode.HEAVY_BLACK_HEART} by $devs")

        event.sendMessage(embed.build()).queue()

    }

}
