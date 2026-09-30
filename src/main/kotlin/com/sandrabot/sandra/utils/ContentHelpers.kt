/*
 * Copyright 2026 Avery Carroll, Logan Devecka, and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.constants.Category
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.entities.LocaleContext

// This file provides some utility methods to reliably
// retrieve common strings from the ContentStore

/**
 * Retrieve the localized [category] name.
 */
fun LocaleContext.getCategoryName(category: Category) = getAny("core.categories.${category.displayName}")

/**
 * Retrieve the localized [command] description.
 */
fun LocaleContext.getCommandDescription(command: Command) = getAny("commands.${command.path}.description")

/**
 * Retrieve the localized [command] base name.
 */
fun LocaleContext.getCommandName(command: Command) = getAny("commands.${command.path}.name")

/**
 * Retrieve the localized [command] full name, useful for subcommands.
 */
fun LocaleContext.getCommandFullName(command: Command): String {
    return command.readablePaths.joinToString(" ", transform = this::getAny)
}

/**
 * Retrieve the localized [command] argument usage.
 */
fun LocaleContext.getCommandUsage(command: Command) = command.arguments.joinToString(" ") { argument ->
    val name = getAny("commands.${command.path}.arguments.${argument.name}.name")
    if (argument.isRequired) "<$name>" else "[$name]"
}
