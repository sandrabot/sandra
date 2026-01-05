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
import net.dv8tion.jda.api.utils.messages.MessageEditData
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class Commands : Command() {

    override suspend fun execute(event: CommandEvent) {

        val topCommands = event.sandra.commands.values.filterNot { it.isSubcommand }
        val grouped = topCommands.groupBy { it.category }.filterNot { (category, commands) ->
            category == Category.CUSTOM || commands.isEmpty()
        }

        val messageData = grouped.mapValues { (category, commands) ->
            MessageCreate(useComponentsV2 = true) {
                val displayTitle = event.get("title")
                container {
                    val displayName = event.getAny("core.categories.${category.displayName}")
                    text("# ${category.emoji.formatted} $displayName $displayTitle")
                    separator(isDivider = false)
                    text(commands.sortedBy { it.name }.joinToString("\n") { command ->
                        val name = event.getAny("commands.${command.name}.name")
                        val description = event.getAny("commands.${command.name}.description")
                        "`/$name` - $description"
                    })
                    separator()
                    text(event.get("footnote"))
                }
                actionRow {
                    stringSelectMenu("select:${event.id}", placeholder = event.get("placeholder")) {
                        val entries = grouped.keys.toMutableSet()
                        if (!event.isOwner) entries -= Category.OWNER
                        entries.forEach { category ->
                            val displayName = event.getAny("core.categories.${category.displayName}")
                            option("$displayName $displayTitle", category.name, emoji = category.emoji)
                        }
                    }
                }
            }
        }

        event.reply(messageData[Category.ESSENTIAL]!!).asEphemeral().queue()

        while (true) withTimeoutOrNull(5.minutes) {
            val selectEvent = event.jda.await<StringSelectInteractionEvent> { it.componentId == "select:${event.id}" }
            val category = Category.valueOf(selectEvent.values.first())
            selectEvent.editMessage(MessageEditData.fromCreateData(messageData[category]!!)).queue()
        } ?: break

        event.hook.deleteOriginal().queue()
    }

}
