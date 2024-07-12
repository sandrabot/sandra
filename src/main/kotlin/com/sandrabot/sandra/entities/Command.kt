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

package com.sandrabot.sandra.entities

import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.managers.CommandManager
import net.dv8tion.jda.api.Permission
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

/**
 * This class defines, configures, and handles the execution of slash commands.
 *
 * All commands that extend this class within the `com.sandrabot.sandra.commands`
 * package are dynamically loaded at startup by the [CommandManager].
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class Command(
    arguments: String = "",
    val subgroup: String? = null,
    val guildOnly: Boolean = false,
    val selfPermissions: Set<Permission> = emptySet(),
    val userPermissions: Set<Permission> = emptySet(),
) {

    /**
     * The command's name. By default, this is the simple class name.
     * You should only override this if the command name must differ
     * from the class name, such as 8ball vs Magic8Ball.
     */
    open val name: String = this::class.simpleName!!.lowercase()

    /**
     * List of command arguments that were configured successfully.
     */
    val arguments: List<Argument> = compileArguments(arguments)

    /**
     * The [Category] that this command belongs to.
     * By default, this is determined by the command's package.
     */
    val category: Category = Category.fromClass(this::class)

    /**
     * Determines whether this command is reserved for developers only.
     * When enabled, this command will only be available in trusted servers.
     */
    val isOwnerOnly: Boolean = category == Category.OWNER

    /**
     * List of this command's immediate subcommands.
     */
    val subcommands: List<Command> = this::class.nestedClasses.filter { it.isSubclassOf(Command::class) }
        .map { (it.createInstance() as Command).also { child -> child.parent = this } }

    /**
     * Lists all nested subcommands within this command.
     */
    val allSubcommands: List<Command> = subcommands + subcommands.flatMap { it.allSubcommands }

    /**
     * Allows nested subcommands to be recursively linked to the top level command.
     */
    var parent: Command? = null
        private set

    /**
     * Determines if this command is a subcommand or not.
     */
    val isSubcommand: Boolean by lazy { parent != null }

    /**
     * The qualified path for this command.
     */
    val path: String by lazy {
        if (isSubcommand) {
            var topLevel = this
            do topLevel = topLevel.parent ?: break while (true)
            buildString {
                append(topLevel.name)
                if (subgroup != null) append('.', subgroup)
                append('.', name)
            }
        } else name
    }

    /**
     * This method defines the command logic and
     * is executed after the command is validated.
     */
    abstract suspend fun execute(event: CommandEvent)

    override fun toString(): String = "Command:$path"

}
