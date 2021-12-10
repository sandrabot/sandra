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
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import kotlin.reflect.full.createInstance

class CommandManager {

    val commands: MutableList<Command>

    val size: Int
        get() = commands.size

    init {
        val commandClasses = Reflections("com.sandrabot.sandra.commands").getSubTypesOf(Command::class.java)
        // Filter out subcommand classes, they are instantiated by their parents
        commands = commandClasses.filterNot { it.isMemberClass }.mapNotNull {
            try {
                it.kotlin.createInstance()
            } catch (t: Throwable) {
                logger.error("Failed to instantiate command $it", t.cause)
                null
            }
        }.toMutableList()
        val childrenSum = commands.sumOf { it.children.size }
        logger.info("Successfully loaded ${commands.size} commands with $childrenSum children")
    }

    operator fun get(name: String): Command? = commands.firstOrNull {
        name.equals(it.name, ignoreCase = true) || it.aliases.any { alias -> name.equals(alias, ignoreCase = true) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommandManager::class.java)
    }

}
