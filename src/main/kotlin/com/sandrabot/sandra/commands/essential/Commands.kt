/*
 * Copyright 2017-2021 Avery Carroll and Logan Devecka
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
import com.sandrabot.sandra.entities.Category
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.entities.Paginator
import com.sandrabot.sandra.events.CommandEvent

@Suppress("unused")
class Commands : Command(name = "commands", aliases = arrayOf("cmds")) {

    override suspend fun execute(event: CommandEvent) {

        // Sort the commands into their respective categories, also sorted alphabetically
        val sortedCommands = Category.values().associateWith { category ->
            event.sandra.commands.commands.filter {
                it.category == category && !it.ownerOnly
            }.sortedBy { it.name }
        }.filterNot { it.key == Category.CUSTOM || it.value.isEmpty() }

        val lang = event.languageContext.withRoot("commands.commands")
        val descriptionPages = mutableListOf<String>()
        val builder = StringBuilder()
        var commandsWritten = 0

        for ((category, list) in sortedCommands) {
            // Begin by appending the category header
            builder.append(category.emote).append(" __**").append(category.displayName)
            builder.append(" ").append(lang.translate("command_title")).append("**__\n")
            for (command in list) {
                builder.append("`").append(event.sandra.prefix).append(command.name).append("` - ")
                builder.append(event.translate("commands.${command.name}.description")).append("\n")
                // Wrap the list of commands into pages
                if (++commandsWritten % 20 == 0) {
                    descriptionPages.add(builder.toString())
                    builder.setLength(0)
                }
            }
            // Append an extra blank line to separate categories
            builder.append("\n")
        }

        // Wrap any remaining text to another page
        if (builder.isNotBlank()) descriptionPages.add(builder.toString())
        val embed = event.embed.setTitle(event.translate("commands.help.extra_help"), Constants.DIRECT_SUPPORT)
        embed.setFooter(lang.translate("more_information", event.sandra.prefix))
        Paginator(event).paginate(descriptionPages.map { embed.setDescription(it).build() })

    }

}
