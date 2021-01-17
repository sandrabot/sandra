/*
 * Copyright 2017-2020 Avery Clifton and Logan Devecka
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
import com.sandrabot.sandra.constants.Website
import com.sandrabot.sandra.entities.Category
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.asEmoteUrl
import com.sandrabot.sandra.utils.format
import com.sandrabot.sandra.utils.sanitize

@Suppress("unused")
class Help : Command(
    name = "help",
    aliases = arrayOf("about", "discord", "h", "info", "invite", "links", "support"),
    arguments = "[command] [subcommands:text]"
) {

    override suspend fun execute(event: CommandEvent) {

        if (event.args.isNotEmpty()) {

            // Create a new context with a root so we don't have to use the full path each time
            val lang = event.languageContext.withRoot("commands.help")

            // The user may potentially be looking for a subcommand
            val maybeCommand = event.arguments.command() ?: run {
                // The command couldn't be found with the given arguments
                event.replyError(lang.translate("not_found", event.sandra.prefix))
                return
            }

            if (maybeCommand.ownerOnly || maybeCommand.category == Category.CUSTOM) {
                event.replyError(lang.translate("not_found", event.sandra.prefix))
                return
            }

            val command = event.arguments.text("subcommands")?.let { subcommands ->
                maybeCommand.findChild(subcommands).first ?: run {
                    // If there are no subcommands to list, just show the parent command
                    if (maybeCommand.children.isEmpty()) return@run maybeCommand
                    // Otherwise display a list of the available subcommands for this command
                    val joined = maybeCommand.children.joinToString("**, **", "**", "**") { it.name }
                    event.replyError(lang.translate("available_subcommands", joined))
                    return
                }
            } ?: maybeCommand

            // Begin putting the embed together, starting with the command path and category
            val author = "${command.path.replace(':', ' ')} • ${command.category.name.toLowerCase()}"
            // The command's category emote is used as the author image
            val embed = event.embed.setAuthor(author, null, command.category.emote.asEmoteUrl())
            embed.setTitle(lang.translate("extra_help"), Constants.DIRECT_SUPPORT)
            // Retrieve the translation for the command's description, this time we don't use our other context
            val descriptionValue = "> ${event.translate("commands.${command.path.replace(':', '.')}.description")}"
            embed.addField("${Emotes.PROMPT} ${lang.translate("description_title")}", descriptionValue, false)
            // Display a field listing the aliases if there are any
            if (command.aliases.isNotEmpty()) {
                // Combine all of the aliases into a string to be displayed
                val join = command.aliases.joinToString("**, **${event.sandra.prefix}", "**${event.sandra.prefix}", "**")
                val aliasesValue = "> ${lang.translate("you_can_use")} $join"
                embed.addField("${Emotes.COMMANDS} ${lang.translate("aliases_title")}", aliasesValue, false)
            }
            // Display a field describing the command usage if there's any arguments
            if (command.arguments.isNotEmpty()) {
                // Combine all of the arguments into a string to be displayed
                val join = command.arguments.joinToString(" ") { it.usage }
                val usageValue = "> **${event.sandra.prefix}${command.name}** $join"
                embed.addField("${Emotes.INFO} ${lang.translate("usage_title")}", usageValue, false)
                // Set the footer as well for context about arguments
                embed.setFooter(lang.translate("required_arguments"))
            }
            event.reply(embed.build())
            return
        }

        // If no arguments were supplied, just show information about the bot
        val lang = event.languageContext.withRoot("commands.help.info_embed")
        val embed = event.embed.setTitle(lang.translate("title"), Website.WEBSITE)
        embed.setThumbnail(event.selfUser.effectiveAvatarUrl)
        // You can never be too safe with users, if anyone ever deletes their accounts it will be handled
        val gabby = event.sandra.retrieveUser(Constants.GABBY)?.asTag?.sanitize() ?: "deleted-user"
        val (avery, logan, blair) = Constants.DEVELOPERS.map { event.sandra.retrieveUser(it)?.format() ?: "deleted-user" }
        // Gabby is specifically formatted differently than the other developers, so don't change that
        embed.setDescription(lang.translate("description", avery, logan, blair, gabby, Constants.TWITTER_GABBY))
        embed.addField(lang.translate("configure", Emotes.CONFIG), lang.translate("configure_content", Website.DASHBOARD), false)
        embed.addField(lang.translate("commands", Emotes.COMMANDS), lang.translate("commands_content", event.sandra.prefix), false)
        embed.addField(lang.translate("invite", Emotes.NOTIFY), lang.translate("invite_content", Constants.DIRECT_INVITE), false)
        embed.addField(lang.translate("support", Emotes.BUBBLES), lang.translate("support_content", Constants.DIRECT_SUPPORT), false)
        event.reply(embed.build())

    }

}
