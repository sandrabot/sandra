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

import com.sandrabot.sandra.entities.Category
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.requests.ErrorResponse
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class Commands : Command() {

    private val embedMap = mutableMapOf<DiscordLocale, Map<Category, List<MessageEmbed>>>()

    override suspend fun execute(event: CommandEvent) {

        val menuId = "commands:select:" + event.encodedInteraction
        val builder = StringSelectMenu.create(menuId).setPlaceholder(event.get("placeholder"))
        val localeEmbeds = embedMap.getOrPut(event.localeContext.locale) { generateEmbeds(event) }
        localeEmbeds.keys.filterNot { it == Category.OWNER && !event.isOwner }.map {
            val label = "${event.getAny("core.categories.${it.displayName}")} ${event.get("command_title")}"
            SelectOption.of(label, it.name).withEmoji(Emoji.fromFormatted(it.emote))
        }.also(builder::addOptions)

        val essential = localeEmbeds[Category.ESSENTIAL] ?: throw AssertionError("Missing essential category")
        event.reply(essential).addActionRow(builder.build()).setEphemeral(true).await()

        while (true) withTimeoutOrNull(1.minutes) {
            // await is a blocking call, this is where we wait for the select event
            val selectEvent = event.sandra.shards.await<StringSelectInteractionEvent>()
            if (menuId == selectEvent.componentId && event.user == selectEvent.user) {
                val newEmbeds = localeEmbeds[Category.valueOf(selectEvent.values.first())]!!
                selectEvent.editMessageEmbeds(newEmbeds).await()
            }
        } ?: break

        // remove all components from the message when we're done
        event.hook.editOriginalComponents().queue(null, ERROR_HANDLER)

    }

    private fun generateEmbeds(event: CommandEvent): Map<Category, List<MessageEmbed>> {
        // gather all top-level commands and sort them by category
        val commands = event.sandra.commands.values.filterNot { it.isSubcommand }
        // remap the list of commands into a list of embeds
        return commands.groupBy { it.category }.mapValues { (category, list) ->
            val descriptions = list.map { command ->
                // build the descriptions and join them into pages of 20 commands each
                val description = event.getAny("commands.${command.name}.description")
                buildString { append("`/", command.name, "` - ", description, "\n") }
            }.chunked(20).map { it.joinToString("") }
            // start putting the page embeds together by reusing the builder
            val categoryText = event.getAny("core.categories.${category.displayName}")
            val embed = event.embed.setThumbnail(event.selfUser.effectiveAvatarUrl).setTitle(
                "${category.emote} $categoryText ${event.get("command_title")}"
            ).setFooter(event.get("more_information"))
            descriptions.map { embed.setDescription(it).build() }
        }.filterNot { (category, list) -> category == Category.CUSTOM || list.isEmpty() }
    }

    private companion object {
        private val ERROR_HANDLER = ErrorHandler().ignore(ErrorResponse.UNKNOWN_INTEGRATION)
    }

}
