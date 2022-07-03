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

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.entities.Argument
import com.sandrabot.sandra.entities.Command
import net.dv8tion.jda.api.interactions.commands.build.*
import java.util.*

fun Command.asCommandData(sandra: Sandra): SlashCommandData? {
    if (isSubcommand) return null // Only root commands may build command data
    val commandPath = path.replace('/', '.')
    val available = sandra.locales.availableLocales
    // TODO Replace with localization maps
    val topNames = available.associateWith { sandra.locales.get(it, "commands.$commandPath.name") }
    val topDescriptions = available.associateWith { sandra.locales.get(it, "commands.$commandPath.description") }
    val data = Commands.slash(topNames[Locale.US]!!, topDescriptions[Locale.US]!!).setGuildOnly(guildOnly)
    if (arguments.isNotEmpty()) data.addOptions(arguments.map { it.asOptionData(sandra, commandPath) })
    if (allSubcommands.isNotEmpty()) allSubcommands.groupBy { it.group }.forEach { (group, commands) ->
        val subcommandData = commands.map { subcommand ->
            val subPath = subcommand.path.replace('/', '.')
            val subNames = available.associateWith { sandra.locales.get(it, "commands.$subPath.name") }
            val subDescriptions = available.associateWith { sandra.locales.get(it, "commands.$subPath.description") }
            val subData = SubcommandData(subNames[Locale.US]!!, subDescriptions[Locale.US]!!)
            subData.addOptions(subcommand.arguments.map { it.asOptionData(sandra, subPath) })
        }
        if (group == null) data.addSubcommands(subcommandData) else {
            val groupNames = available.associateWith { sandra.locales.get(it, "commands.$commandPath.$group.name") }
            val groupDescriptions = available.associateWith {
                sandra.locales.get(it, "commands.$commandPath.$group.description")
            }
            val groupData = SubcommandGroupData(groupNames[Locale.US]!!, groupDescriptions[Locale.US]!!)
            groupData.addSubcommands(subcommandData)
            data.addSubcommandGroups(groupData)
        }
    }
    return data
}

fun Argument.asOptionData(sandra: Sandra, path: String): OptionData {
    // TODO Replace with localization maps
    val translations = sandra.locales.availableLocales.associateWith {
        sandra.locales.getList(it, "commands.$path.arguments.$name")
    }
    val optionData = OptionData(
        type.optionType, translations[Locale.US]?.get(0)!!, translations[Locale.US]?.get(1)!!, isRequired
    )
    options.forEachIndexed { index, any ->
        when (any) {
            is String -> optionData.addChoice(translations[Locale.US]?.get(index + 2)!!, any)
            is Long -> optionData.addChoice(translations[Locale.US]?.get(index + 2)!!, any)
            is Double -> optionData.addChoice(translations[Locale.US]?.get(index + 2)!!, any)
        }
    }
    return optionData
}
