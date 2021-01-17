/*
 * Copyright 2017-2020 Avery Clifton and Logan Devecka
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

import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.removeExtraSpaces
import com.sandrabot.sandra.utils.splitSpaces
import net.dv8tion.jda.api.Permission
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

abstract class Command(
    val name: String,
    val aliases: Array<String> = emptyArray(),
    arguments: String = "",
    val guildOnly: Boolean = false,
    val ownerOnly: Boolean = false,
    val cooldown: Int = Constants.DEFAULT_COOLDOWN,
    val cooldownScope: CooldownScope = CooldownScope.USER,
    val botPermissions: Array<Permission> = emptyArray(),
    val userPermissions: Array<Permission> = emptyArray()
) {

    val arguments: List<Argument> = Argument.compile(arguments)
    val category: Category = Category.fromClass(this::class)
    val children: List<Command> = this::class.nestedClasses.filter {
        it.isSubclassOf(Command::class)
    }.map {
        (it.createInstance() as Command).also { child -> child.parent = this }
    }.toList()

    val path: String by lazy {
        var currentCommand = this
        val builder = StringBuilder()
        do {
            builder.insert(0, currentCommand.name + ":")
            currentCommand = currentCommand.parent ?: break
        } while (true)
        builder.substring(0, builder.lastIndex)
    }

    var parent: Command? = null
        internal set
    val isSubcommand: Boolean
        get() = parent != null

    abstract suspend fun execute(event: CommandEvent)

    /**
     * Finds children of this command by recursively walking the command tree.
     * The returned pair is the possible child and the remaining arguments.
     * If no child was found the command will be null.
     * Otherwise, it is guaranteed that the remaining arguments have changed.
     */
    fun findChild(args: String): Pair<Command?, String> {
        // Attempt to find a child with the first word as the alias
        val firstArg = args.splitSpaces().first()
        val child = children.firstOrNull {
            arrayOf(it.name, *it.aliases).any { alias ->
                firstArg.equals(alias, ignoreCase = true)
            }
        }
        var arguments = args
        // Use recursion to continue walking the command tree
        val nestedCommand = if (child != null) {
            arguments = arguments.removePrefix(firstArg).removeExtraSpaces()
            // If there was only one word there's nothing else to find
            if (arguments.isNotEmpty()) {
                val recursive = child.findChild(arguments)
                // Reassign the arguments if a command was found and a word was used
                if (recursive.first != null) {
                    arguments = recursive.second
                    recursive.first
                } else child
            } else child
        } else child
        return nestedCommand to arguments
    }

}
