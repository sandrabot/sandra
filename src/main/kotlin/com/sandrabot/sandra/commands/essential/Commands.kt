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
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.utils.getCategoryName
import com.sandrabot.sandra.utils.getCommandDescription
import com.sandrabot.sandra.utils.getCommandName
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.utils.messages.MessageEditData
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class Commands : Command() {

    override suspend fun execute(event: CommandEvent) {

        // create a list of all available top-level commands
        val topCommands = event.sandra.commands.values.filterNot { it.isSubcommand }
        // group the commands by category, hide custom commands and owner commands
        val groupedCommands = topCommands.groupBy { it.category }.filterNot { (category, _) ->
            category == Category.CUSTOM || (category == Category.OWNER && !event.isOwner)
        }

        // use the command name "commands" to display titles as well
        val commandsTitle = event.get("name")
        // build the string selection menu for changing pages between categories
        val selectMenu = StringSelectMenu("select:${event.id}", placeholder = event.get("placeholder")) {
            // add all available categories as possible menu options
            groupedCommands.keys.sortedBy { it.name }.forEach { category ->
                val categoryName = event.localeContext.getCategoryName(category)
                option("$categoryName $commandsTitle", category.name, emoji = category.emoji)
            }
        }

        // create a map of localized message data for each available category
        val messageData = groupedCommands.mapValues { (category, commands) ->
            MessageCreate(useComponentsV2 = true) {
                container {
                    val categoryName = event.localeContext.getCategoryName(category)
                    text("# ${category.emoji.formatted} $categoryName $commandsTitle")
                    separator(isDivider = false)
                    text(commands.sortedBy { it.name }.joinToString("\n") { command ->
                        // use the command's base name since we're not listing subcommands
                        val name = event.localeContext.getCommandName(command)
                        val description = event.localeContext.getCommandDescription(command)
                        "`/$name` - $description"
                    })
                    separator()
                    text(event.get("footnote"))
                }
                actionRow(selectMenu)
            }
        }

        // respond to the interaction with the "essential" command category as default
        event.reply(messageData[Category.ESSENTIAL]!!).asEphemeral().queue()

        // wait for 5 minutes after each interaction for the user to make another selection
        while (true) withTimeoutOrNull(5.minutes) {
            // blocking call to await the next interaction event that matches the select menu
            val selectEvent = event.jda.await<StringSelectInteractionEvent> { it.componentId == selectMenu.customId }
            // figure out the new category from the selected value
            val newCategory = Category.valueOf(selectEvent.values.first())
            // edit the original message with the message data for the new category
            selectEvent.editMessage(MessageEditData.fromCreateData(messageData[newCategory]!!)).queue()
        } ?: break

        // delete the original message after the 5-minute timeout
        event.hook.deleteOriginal().queue()
    }

}
