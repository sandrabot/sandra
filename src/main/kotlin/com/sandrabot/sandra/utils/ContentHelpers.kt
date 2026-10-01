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
import com.sandrabot.sandra.entities.Argument
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.entities.LocaleContext
import net.dv8tion.jda.api.Permission

// This file provides some utility methods to reliably
// retrieve common strings from the ContentStore

/**
 * Retrieve the localized [argument] name for the specified [command].
 */
fun LocaleContext.getArgumentName(command: Command, argument: Argument): String {
    return getAny("commands.${command.path}.arguments.${argument.name}.name")
}

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
    val name = getArgumentName(command, argument)
    if (argument.isRequired) "<$name>" else "[$name]"
}

/**
 * Retrieve the localized interaction error message.
 */
fun LocaleContext.getInteractionError() = getAny("core.interaction_error")

/**
 * Retrieve the localized missing argument error message.
 */
fun LocaleContext.getMissingArgumentError(command: Command, argument: Argument): String {
    return getAny("core.missing_argument", getArgumentName(command, argument))
}

/**
 * Retrieve the localized missing permission error message.
 *
 * When [isSelf] is set to `true`, the message will say that the bot is missing the permission.
 * When set to `false`, the message will convey that the user lacks the permission to continue.
 */
fun LocaleContext.getMissingPermissionError(permission: Permission, isSelf: Boolean): String {
    val path = if (isSelf) "core.missing_self_permission" else "core.missing_user_permission"
    val name = getAny("core.permissions.${permission.translationKey}")
    val context = getAny(if (permission.isChannel) "core.channel" else "core.server")
    return getAny(path, name, context)
}

/**
 * Returns the translation key to retrieve a localized name for common permissions.
 */
val Permission.translationKey
    get() = when (this) {
        Permission.ADMINISTRATOR -> "administrator"
        Permission.BAN_MEMBERS -> "ban"
        Permission.CREATE_INSTANT_INVITE -> "create_invite"
        Permission.KICK_MEMBERS -> "kick"
        Permission.MANAGE_PERMISSIONS -> "manage_permissions"
        Permission.MANAGE_ROLES -> "manage_roles"
        Permission.MESSAGE_ADD_REACTION -> "add_reactions"
        Permission.MESSAGE_ATTACH_FILES -> "attach_files"
        Permission.MESSAGE_EMBED_LINKS -> "embed_links"
        Permission.MESSAGE_EXT_EMOJI -> "external_emojis"
        Permission.MESSAGE_HISTORY -> "read_history"
        Permission.MESSAGE_MANAGE -> "manage_messages"
        Permission.MESSAGE_SEND -> "send_messages"
        Permission.MODERATE_MEMBERS -> "moderate_members"
        Permission.NICKNAME_CHANGE -> "change_nickname"
        Permission.NICKNAME_MANAGE -> "manage_nicknames"
        Permission.VIEW_AUDIT_LOGS -> "view_audit_logs"
        Permission.VIEW_CHANNEL -> "view_channel"
        Permission.VOICE_CONNECT -> "voice_connect"
        Permission.VOICE_SPEAK -> "voice_speak"
        Permission.VOICE_USE_VAD -> "voice_activity"
        else -> throw IllegalArgumentException("Missing permission translation key: $this")
    }
