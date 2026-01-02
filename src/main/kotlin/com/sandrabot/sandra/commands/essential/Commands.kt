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
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class Commands : Command() {

    private val messageDataMap = mutableMapOf<DiscordLocale, Map<Category, MessageCreateData>>()

    override suspend fun execute(event: CommandEvent) {
        val messageData = messageDataMap.getOrPut(event.localeContext.locale) { generateMessageData(event) }
        event.reply(messageData[Category.ESSENTIAL]!!).asEphemeral().queue()

        while (true) withTimeoutOrNull(5.minutes) {
            val selectEvent = event.sandra.shards.await<StringSelectInteractionEvent>()
            if (selectEvent.componentId == "select:${event.id}") {
                val category = Category.valueOf(selectEvent.values.first())
                selectEvent.editMessage(MessageEditData.fromCreateData(messageData[category]!!)).queue()
            }
        } ?: break

        event.hook.deleteOriginal().queue()
    }

    private fun generateMessageData(event: CommandEvent): Map<Category, MessageCreateData> {
        val commands = event.sandra.commands.values.filterNot { it.isSubcommand }
        val sorted = commands.groupBy { it.category }.filterNot { (c, l) -> c == Category.CUSTOM || l.isEmpty() }
        return sorted.mapValues { (category, list) ->
            MessageCreate(useComponentsV2 = true) {
                container {
                    val displayName = event.getAny("core.categories.${category.displayName}")
                    text("# ${category.emoji.formatted} $displayName ${event.get("title")}")
                    separator(isDivider = false)
                    text(list.sortedBy { it.name }.joinToString("\n") { command ->
                        val description = event.getAny("commands.${command.name}.description")
                        "`/ ${command.name}` - $description"
                    })
                    separator()
                    text(event.get("footnote"))
                }
                actionRow {
                    stringSelectMenu("select:${event.id}", placeholder = event.get("placeholder")) {
                        val entries = sorted.keys.toMutableSet()
                        if (!event.isOwner) entries -= Category.OWNER
                        entries.forEach { category ->
                            val displayName = event.getAny("core.categories.${category.displayName}")
                            val label = displayName + " " + event.get("title")
                            option(label, category.name, emoji = category.emoji)
                        }
                    }
                }
            }
        }
    }

}
