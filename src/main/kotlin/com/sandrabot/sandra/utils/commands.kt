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

import com.sandrabot.sandra.constants.ContentStore
import com.sandrabot.sandra.entities.Argument
import com.sandrabot.sandra.entities.Command
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.DiscordLocale.ENGLISH_US
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.*

typealias ContentMap = Map<DiscordLocale, String>

/**
 * Converts and collects all metadata for this [Command]
 * into a [SlashCommandData] object that JDA can use.
 */
fun Command.asCommandData(): SlashCommandData? {
    if (isSubcommand) return null
    val path = "commands.$path"
    // compute a map of all possible names and descriptions for this command
    val (names, descriptions) = associateContent(path)
    val data = Commands.slash(names[ENGLISH_US]!!, descriptions[ENGLISH_US]!!).setGuildOnly(guildOnly)
    // convert locales to discord locales and set the translations for the command
    data.setNameLocalizations(names).setDescriptionLocalizations(descriptions)
    // set the default permissions for this command
    if (userPermissions.isNotEmpty()) data.defaultPermissions = DefaultMemberPermissions.enabledFor(userPermissions)
    // add the argument data here if applicable
    if (arguments.isNotEmpty()) data.addOptions(arguments.map { it.asOptionData(path) })
    // process any subcommands this command may have
    if (allSubcommands.isNotEmpty()) allSubcommands.groupBy { it.group }.forEach { (group, commands) ->
        val subcommandData = commands.map { subcommand ->
            val subPath = "commands.${subcommand.path}"
            // retrieve a map of all possible names and descriptions
            val (subNames, subDesc) = associateContent(subPath)
            val subData = SubcommandData(subNames[ENGLISH_US]!!, subDesc[ENGLISH_US]!!)
            // convert locales to discord locales and set the translations for the subcommand
            subData.setNameLocalizations(subNames).setDescriptionLocalizations(subDesc)
            subData.addOptions(subcommand.arguments.map { it.asOptionData(subPath) })
        }
        if (group == null) data.addSubcommands(subcommandData) else {
            // retrieve a map of all possible names and descriptions
            val (groupNames, groupDesc) = associateContent("$path.$group")
            val groupData = SubcommandGroupData(groupNames[ENGLISH_US]!!, groupDesc[ENGLISH_US]!!)
            // convert locales to discord locales and set the translations for the subcommand group
            groupData.setNameLocalizations(groupNames).setDescriptionLocalizations(groupDesc)
            groupData.addSubcommands(subcommandData)
            data.addSubcommandGroups(groupData)
        }
    }
    return data
}

fun Argument.asOptionData(commandPath: String): OptionData {
    val path = "$commandPath.arguments.$name"
    // compute a map of all possible names and descriptions for this option
    val (optionNames, optionDesc) = associateContent(path)
    val data = OptionData(type.optionType, optionNames[ENGLISH_US]!!, optionDesc[ENGLISH_US]!!, isRequired)
    if (choices.isNotEmpty()) {
        val choiceNames = ContentStore.locales.associateWith { ContentStore.getList(it, "$path.choices") }
        choices.forEachIndexed { index, any -> data.addChoice(choiceNames[ENGLISH_US]?.get(index)!!, any!!) }
    }
    // convert locales to discord locales and set the translations for the option
    data.setNameLocalizations(optionNames).setDescriptionLocalizations(optionDesc)
    return data
}

private fun OptionData.addChoice(name: String, value: Any) = when (value) {
    is Double -> addChoice(name, value)
    is Long -> addChoice(name, value)
    is String -> addChoice(name, value)
    else -> throw AssertionError("Option choice $name is of value $value")
}

private fun associateContent(path: String): Pair<ContentMap, ContentMap> {
    // compute a map of all possible names and descriptions for this object
    val names = ContentStore.locales.associateWith { ContentStore[it, "$path.name"] }
    val descriptions = ContentStore.locales.associateWith { ContentStore[it, "$path.description"] }
    // convert locales to discord locales and set the translations for the object
    return names to descriptions
}
