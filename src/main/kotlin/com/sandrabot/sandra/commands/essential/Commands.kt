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
import com.sandrabot.sandra.events.asEphemeral
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.option
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class Commands : Command() {

    private val embedMap = mutableMapOf<DiscordLocale, Map<Category, List<MessageEmbed>>>()

    override suspend fun execute(event: CommandEvent) {

        // TODO we should map with MessageCreateData and use v2 components
        val localeEmbeds = embedMap.getOrPut(event.localeContext.locale) { generateEmbeds(event) }
        val selectMenu = StringSelectMenu("select:${event.id}", placeholder = event.get("placeholder")) {
            localeEmbeds.keys.filterNot { it == Category.OWNER && !event.isOwner }.map {
                val label = event.getAny("core.categories.${it.displayName}") + " " + event.get("command_title")
                option(label, it.name, emoji = Emoji.fromFormatted(it.emote))
            }
        }

        val embeds = localeEmbeds[Category.ESSENTIAL] ?: throw IllegalStateException("Missing essential category")
        event.replyEmbeds(embeds).addComponents(ActionRow.of(selectMenu)).asEphemeral().await()

        // allow the user to make any number of selections until 1 minute from the last interaction
        while (true) withTimeoutOrNull(1.minutes) {
            val selectEvent = event.sandra.shards.await<StringSelectInteractionEvent>()
            if (selectMenu.customId == selectEvent.componentId) {
                val newEmbeds = localeEmbeds[Category.valueOf(selectEvent.values.first())]!!
                selectEvent.editMessageEmbeds(newEmbeds).await()
            }
        } ?: break

        // remove all components from the message when we're done
        event.hook.editOriginalComponents().queue(null, null)

    }

    private fun generateEmbeds(event: CommandEvent): Map<Category, List<MessageEmbed>> {
        // gather all top-level commands and sort them by category
        val commands = event.sandra.commands.values.filterNot { it.isSubcommand }
        // remap the list of commands into a list of embeds
        return commands.groupBy { it.category }.mapValues { (category, list) ->
            val descriptions = list.sortedBy { it.name }.map { command ->
                // build the descriptions and join them into pages of 20 commands each
                val description = event.getAny("commands.${command.name}.description")
                buildString { append("`/", command.name, "` - ", description, "\n") }
            }.chunked(20).map { it.joinToString("") }
            // start putting the page embeds together by reusing the builder
            val categoryText = event.getAny("core.categories.${category.displayName}")
            val embed = event.embed().setThumbnail(event.selfUser.effectiveAvatarUrl).setTitle(
                "${category.emote} $categoryText ${event.get("command_title")}"
            ).setFooter(event.get("more_information"))
            descriptions.map { embed.setDescription(it).build() }
        }.filterNot { (category, list) -> category == Category.CUSTOM || list.isEmpty() }
    }

}
