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

package com.sandrabot.sandra.managers

import com.sandrabot.sandra.entities.Command
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import kotlin.reflect.full.createInstance

class CommandManager {

    val commands = mutableMapOf<String, Command>()

    init {
        val commandClasses = Reflections("com.sandrabot.sandra.commands").getSubTypesOf(Command::class.java)
        // Filter out subcommand classes, they are instantiated by their parents
        commandClasses.filterNot { it.isMemberClass }.mapNotNull {
            try {
                val command = it.kotlin.createInstance()
                commands[command.path] = command
            } catch (t: Throwable) {
                logger.error("Failed to instantiate command $it", t)
                null
            }
        }
        // Create a copy of the values to prevent CME
        loadChildrenRecursive(commands.values.toList())
        val childrenSum = commands.count { it.value.isSubcommand }
        logger.info("Successfully loaded ${commands.size - childrenSum} commands with $childrenSum children")
    }

    private fun loadChildrenRecursive(list: Collection<Command>) {
        for (command in list) {
            command.subcommands.ifNotEmpty {
                forEach { commands[it.path] = it }
                loadChildrenRecursive(this)
            }
        }
    }

    operator fun get(path: String): Command? = commands[path]

    companion object {
        private val logger = LoggerFactory.getLogger(CommandManager::class.java)
    }

}
