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

package com.sandrabot.sandra.entities

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.events.CommandEvent
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.build.*
import java.util.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

abstract class Command(
    val name: String,
    arguments: String = "",
    val guildOnly: Boolean = false,
    val group: String? = null,
    val requiredPermissions: Array<Permission> = emptyArray(),
    val userPermissions: Array<Permission> = emptyArray()
) {

    val arguments: List<Argument> = compileArguments(arguments)
    val category: Category = Category.fromClass(this::class)
    val ownerOnly: Boolean = category == Category.OWNER
    val subcommands: List<Command> = this::class.nestedClasses.filter { it.isSubclassOf(Command::class) }
        .map { (it.createInstance() as Command).also { child -> child.parent = this } }
    val allSubcommands: List<Command> = subcommands + subcommands.flatMap { it.allSubcommands }

    // Must be lazy so parents are set before access
    val path: String by lazy {
        if (!isSubcommand) name else {
            var topLevelParent = this
            do topLevelParent = topLevelParent.parent ?: break while (true)
            buildString {
                append(topLevelParent.name)
                if (group != null) append('/', group)
                append('/', name)
            }
        }
    }

    var id: Long = 0L
        set(value) {
            // Only allow the field to be set once for top level commands
            if (field == 0L && !isSubcommand) field = value
            else throw IllegalStateException("Command ID for $path has already been set to $field")
        }
    var parent: Command? = null
        internal set
    val isSubcommand: Boolean
        get() = parent != null

    abstract suspend fun execute(event: CommandEvent)

    fun asCommandData(sandra: Sandra): CommandData? {
        if (isSubcommand) return null // Only root commands may build command data
        val commandPath = path.replace('/', '.')
        val data = Commands.slash(name, sandra.locales.get(Locale.US, "commands.$commandPath.description"))
        if (ownerOnly) data.isDefaultEnabled = false
        if (arguments.isNotEmpty()) data.addOptions(arguments.asOptions(sandra, commandPath))
        if (allSubcommands.isNotEmpty()) allSubcommands.groupBy { it.group }.forEach { (group, commands) ->
            val subcommandData = commands.map {
                val subPath = it.path.replace('/', '.')
                val description = sandra.locales.get(Locale.US, "commands.$subPath.description")
                val subData = SubcommandData(it.name, description)
                subData.addOptions(it.arguments.asOptions(sandra, subPath))
            }
            if (group == null) data.addSubcommands(subcommandData) else {
                val description = sandra.locales.get(Locale.US, "commands.$commandPath.$group.description")
                val groupData = SubcommandGroupData(group, description)
                groupData.addSubcommands(subcommandData)
                data.addSubcommandGroups(groupData)
            }
        }
        return data
    }

    // TODO Fix argument localization
    private fun List<Argument>.asOptions(sandra: Sandra, path: String): List<OptionData> = map {
        val translations = sandra.locales.getList(Locale.US, "commands.$path.arguments.${it.name}")
        val optionData = OptionData(it.type.optionType, it.name, translations[0], it.isRequired)
        it.options.forEachIndexed { index, any ->
            when (any) {
                is String -> optionData.addChoice(translations[index + 1], any)
                is Long -> optionData.addChoice(translations[index + 1], any)
                is Double -> optionData.addChoice(translations[index + 1], any)
            }
        }
        optionData
    }

    override fun toString(): String = "Command:$path"

}
