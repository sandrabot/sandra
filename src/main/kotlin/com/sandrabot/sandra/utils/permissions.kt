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

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.exceptions.MissingPermissionException
import com.sandrabot.sandra.exceptions.MissingTranslationException
import net.dv8tion.jda.api.Permission

fun missingPermission(event: CommandEvent, permission: Permission) = !hasPermission(event, permission)
fun hasPermission(event: CommandEvent, permission: Permission): Boolean {
    return if (permission.isChannel) {
        event.selfMember.hasPermission(event.textChannel, permission)
    } else event.selfMember.hasPermission(permission)
}

fun assertPermission(event: CommandEvent, permission: Permission) {
    if (missingPermission(event, permission)) throw MissingPermissionException(event, permission)
}

fun missingSelfMessage(event: CommandEvent, permission: Permission): String {
    val context = if (permission.isChannel) "channel" else "server"
    return event.translate(
            "general.missing_permission",
            event.languageContext.get("permissions.${findTranslationKey(permission)}"),
            context
    )
}

fun findTranslationKey(permission: Permission) = when (permission) {
    Permission.CREATE_INSTANT_INVITE -> "create_invite"
    Permission.KICK_MEMBERS -> "kick"
    Permission.BAN_MEMBERS -> "ban"
    Permission.ADMINISTRATOR -> "administrator"
    Permission.MESSAGE_ADD_REACTION -> "add_reactions"
    Permission.VIEW_AUDIT_LOGS -> "view_audit_logs"
    Permission.MESSAGE_READ -> "read_messages"
    Permission.MESSAGE_WRITE -> "send_messages"
    Permission.MESSAGE_MANAGE -> "manage_messages"
    Permission.MESSAGE_EMBED_LINKS -> "embed_links"
    Permission.MESSAGE_ATTACH_FILES -> "attach_files"
    Permission.MESSAGE_HISTORY -> "read_history"
    Permission.MESSAGE_EXT_EMOJI -> "external_emojis"
    Permission.VOICE_CONNECT -> "voice_connect"
    Permission.VOICE_SPEAK -> "voice_speak"
    Permission.VOICE_USE_VAD -> "voice_activity"
    Permission.NICKNAME_CHANGE -> "change_nickname"
    Permission.NICKNAME_MANAGE -> "manage_nicknames"
    Permission.MANAGE_ROLES -> "manage_roles"
    Permission.MANAGE_PERMISSIONS -> "manage_permissions"
    else -> throw MissingTranslationException("Missing translation for permission $permission")
}
