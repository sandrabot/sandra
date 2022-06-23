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

import com.sandrabot.sandra.entities.Category
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.requests.ErrorResponse
import java.util.concurrent.TimeUnit

@Suppress("unused")
class Commands : Command(name = "commands") {

    override suspend fun execute(event: CommandEvent) {

        val selectionMenu = getSelectionMenu(event)
        event.reply(getEmbeds(event, Category.ESSENTIAL)).addActionRow(selectionMenu)
            .setEphemeral(true).queue({ waitForSelection(event, selectionMenu) }, handler)

    }

    private fun waitForSelection(event: CommandEvent, selectionMenu: SelectMenu) {
        event.sandra.eventWaiter.waitForEvent(SelectMenuInteractionEvent::class,
            timeout = 2, unit = TimeUnit.MINUTES,
            expired = { event.hook.editOriginalComponents().queue(null, handler) },
            test = { it.componentId == componentPrefix + event.encodedInteraction && it.user.idLong == event.user.idLong }
        ) {
            val embeds = getEmbeds(event, Category.valueOf(it.values.first()))
            it.editMessageEmbeds(embeds).queue { waitForSelection(event, selectionMenu) }
        }
    }

    private companion object {
        private const val componentPrefix = "commands:select:"
        private val handler = ErrorHandler().ignore(ErrorResponse.UNKNOWN_INTERACTION)
        private val embeds = mutableMapOf<Category, List<MessageEmbed>>()
        private var menuBuilder: SelectMenu.Builder? = null

        private fun Category.path() = "categories." + name.lowercase()

        private fun getSelectionMenu(event: CommandEvent): SelectMenu = (menuBuilder ?: run {
            SelectMenu.create(componentPrefix).setPlaceholder(event.translate("select_placeholder"))
                .addOptions(event.sandra.commands.values.groupBy { it.category }.filterNot { (category, list) ->
                    category == Category.CUSTOM || category == Category.OWNER || list.isEmpty()
                }.toSortedMap().map { (category, _) ->
                    val displayName = event.translate(category.path(), false)
                    SelectOption.of(displayName + " " + event.translate("command_title"), category.name)
                        .withEmoji(Emoji.fromMarkdown(category.emote))
                }).also { menuBuilder = it }
        }).setId(componentPrefix + event.encodedInteraction).build()

        private fun getEmbeds(event: CommandEvent, category: Category): List<MessageEmbed> =
            embeds.getOrPut(category) { buildEmbeds(event, category) }

        private fun buildEmbeds(event: CommandEvent, category: Category): List<MessageEmbed> {
            // Filter for top level commands that are from this category
            val commands = event.sandra.commands.values.filter {
                it.category == category && !it.isSubcommand
            }.sortedBy { it.name }
            val embedDescriptions = commands.map {
                // Append each command to the embed description
                val commandDescription = event.translate("commands.${it.name}.description", false)
                buildString { append("`/", it.name, "` - ", commandDescription, "\n") }
            }.chunked(20).map { it.joinToString("") } // Chunk the commands into groups and combine them
            val embed = event.embed.setTitle(
                "${category.emote} ${event.translate(category.path(), false)} ${event.translate("command_title")}"
            ).setThumbnail(event.selfUser.effectiveAvatarUrl).setFooter(event.translate("more_information"))
            // Build each page using the embed template and its own description
            return embedDescriptions.map { embed.setDescription(it).build() }
        }
    }

}
