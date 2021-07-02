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

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.exceptions.MissingPermissionException
import com.sandrabot.sandra.exceptions.MissingTranslationException
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member

fun missingPermission(event: CommandEvent, permission: Permission) = !hasPermission(event, permission)
fun hasPermission(event: CommandEvent, permission: Permission) = check(event, permission, event.selfMember)

fun missingUserPermission(event: CommandEvent, permission: Permission) = !hasUserPermission(event, permission)
fun hasUserPermission(event: CommandEvent, permission: Permission) = check(event, permission, event.member)

private fun check(event: CommandEvent, permission: Permission, member: Member): Boolean {
    return if (permission.isChannel) {
        member.hasPermission(event.textChannel, permission)
    } else member.hasPermission(permission)
}

fun missingPermissions(event: CommandEvent, vararg permissions: Permission) = !hasPermissions(event, *permissions)
fun hasPermissions(event: CommandEvent, vararg permissions: Permission): Boolean {
    return permissions.all { hasPermission(event, it) }
}

fun ensurePermissions(event: CommandEvent, vararg permissions: Permission) =
    permissions.forEach { ensurePermission(event, it) }

fun ensurePermission(event: CommandEvent, permission: Permission) {
    if (missingPermission(event, permission)) throw MissingPermissionException(event, permission)
}

fun missingSelfMessage(event: CommandEvent, permission: Permission) = missingMessage(event, permission, true)
fun missingUserMessage(event: CommandEvent, permission: Permission) = missingMessage(event, permission, false)
private fun missingMessage(event: CommandEvent, permission: Permission, self: Boolean): String {
    val context = if (permission.isChannel) "channel" else "server"
    return event.translate(
        if (self) "general.missing_permission" else "general.missing_user_permission",
        event.localeContext.get("permissions.${findTranslationKey(permission)}", false), context
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
