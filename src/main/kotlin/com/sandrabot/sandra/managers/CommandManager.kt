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

package com.sandrabot.sandra.managers

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.utils.asCommandData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import kotlin.reflect.full.createInstance

/**
 * Loads and stores all [Command] objects that are available at runtime.
 */
class CommandManager(sandra: Sandra) {

    private val commands = mutableMapOf<String, Command>()

    /**
     * Contains accurate [SlashCommandData] metadata for all top-level commands.
     * Use this collection when updating the Discord commands at startup.
     */
    val commandData = mutableMapOf<String, SlashCommandData>()

    /**
     * Shorthand for retrieving the list of current commands.
     * Please note this collection is **mutable**, any changes should be done with care.
     */
    val values: MutableCollection<Command>
        get() = commands.values

    init {
        val reflections = Reflections("com.sandrabot.sandra.commands").getSubTypesOf(Command::class.java)
        // filter out any subcommand classes, since they are initialized by their parents
        reflections.filterNot { it.isMemberClass }.mapNotNull { clazz ->
            try {
                // attempt to load and create an instance of this command
                val command = clazz.kotlin.createInstance()
                // validate the command's metadata before allowing it to be used
                commandData[command.path] = command.asCommandData(sandra)
                    ?: throw AssertionError("Command data was null for top level command")
                if (command.path in commands)
                    throw IllegalArgumentException("Duplicate command path ${command.path} is already in use")
                commands[command.path] = command
            } catch (t: Throwable) {
                logger.error("An exception occurred while loading command $clazz", t)
                null
            }
        }
        // add all the subcommands into the main command map
        // this allows them to be retrieved much easier by the command listener
        commands.values.flatMap { it.allSubcommands }.forEach { commands[it.path] = it }
        val children = commands.count { it.value.isSubcommand }
        logger.info("Successfully loaded ${commands.size - children} commands with $children subcommands")
    }

    /**
     * Returns the command at [path], or `null` if the command does not exist.
     */
    operator fun get(path: String): Command? = commands[path]

    private companion object {
        private val logger = LoggerFactory.getLogger(CommandManager::class.java)
    }

}
