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
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.*
import java.util.*

/**
 * Converts and collects all metadata for this [Command]
 * into a [SlashCommandData] object that JDA can use.
 */
fun Command.asCommandData(sandra: Sandra): SlashCommandData? {
    if (isSubcommand) return null // only root commands are able to build command data
    val path = "commands." + path.replace('/', '.')
    val available = sandra.lang.availableLocales
    // compute a map of all possible names and descriptions for this command
    val commandNames = available.associateWith { sandra.lang.get(it, "$path.name") }
    val commandDescriptions = available.associateWith { sandra.lang.get(it, "$path.description") }
    val data = Commands.slash(commandNames[Locale.US]!!, commandDescriptions[Locale.US]!!).setGuildOnly(guildOnly)
    // convert locales to discord locales and set the translations for the command
    data.setNameLocalizations(commandNames.mapKeys { (locale, _) -> DiscordLocale.from(locale) })
    data.setDescriptionLocalizations(commandDescriptions.mapKeys { (locale, _) -> DiscordLocale.from(locale) })
    // add the argument data here if applicable
    if (arguments.isNotEmpty()) data.addOptions(arguments.map { it.asOptionData(sandra, path) })
    // process any subcommands this command may have
    if (allSubcommands.isNotEmpty()) allSubcommands.groupBy { it.group }.forEach { (group, commands) ->
        val subcommandData = commands.map { subcommand ->
            val subPath = "commands." + subcommand.path.replace('/', '.')
            // retrieve a map of all possible names and descriptions
            val subNames = available.associateWith { sandra.lang.get(it, "$subPath.name") }
            val subDescriptions = available.associateWith { sandra.lang.get(it, "$subPath.description") }
            val subData = SubcommandData(subNames[Locale.US]!!, subDescriptions[Locale.US]!!)
            // convert locales to discord locales and set the translations for the subcommand
            subData.setNameLocalizations(subNames.mapKeys { (locale, _) -> DiscordLocale.from(locale) })
            subData.setDescriptionLocalizations(subDescriptions.mapKeys { (locale, _) -> DiscordLocale.from(locale) })
            subData.addOptions(subcommand.arguments.map { it.asOptionData(sandra, subPath) })
        }
        if (group == null) data.addSubcommands(subcommandData) else {
            // retrieve a map of all possible names and descriptions
            val groupNames = available.associateWith { sandra.lang.get(it, "$path.$group.name") }
            val groupDescriptions = available.associateWith { sandra.lang.get(it, "$path.$group.description") }
            val groupData = SubcommandGroupData(groupNames[Locale.US]!!, groupDescriptions[Locale.US]!!)
            // convert locales to discord locales and set the translations for the subcommand group
            groupData.setNameLocalizations(groupNames.mapKeys { (locale, _) -> DiscordLocale.from(locale) })
            groupData.setDescriptionLocalizations(groupDescriptions.mapKeys { (locale, _) -> DiscordLocale.from(locale) })
            groupData.addSubcommands(subcommandData)
            data.addSubcommandGroups(groupData)
        }
    }
    return data
}

fun Argument.asOptionData(sandra: Sandra, commandPath: String): OptionData {
    val path = "$commandPath.arguments.$name"
    val available = sandra.lang.availableLocales
    // compute a map of all possible names and descriptions for this option
    val optionNames = available.associateWith { sandra.lang.get(it, "$path.name") }
    val optionDescriptions = available.associateWith { sandra.lang.get(it, "$path.description") }
    val data = OptionData(type.optionType, optionNames[Locale.US]!!, optionDescriptions[Locale.US]!!, isRequired)
    if (choices.isNotEmpty()) {
        val choiceNames = available.associateWith { sandra.lang.getList(it, "$path.choices") }
        choices.forEachIndexed { index, any -> data.addChoice(choiceNames[Locale.US]?.get(index)!!, any!!) }
    }
    // convert locales to discord locales and set the translations for the option
    data.setNameLocalizations(optionNames.mapKeys { (locale, _) -> DiscordLocale.from(locale) })
    data.setDescriptionLocalizations(optionDescriptions.mapKeys { (locale, _) -> DiscordLocale.from(locale) })
    return data
}

private fun OptionData.addChoice(name: String, value: Any) = when (value) {
    is Double -> addChoice(name, value)
    is Long -> addChoice(name, value)
    is String -> addChoice(name, value)
    else -> throw AssertionError("Option choice $name is of value $value")
}
