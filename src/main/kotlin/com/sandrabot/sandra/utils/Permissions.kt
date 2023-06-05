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

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.exceptions.MissingPermissionException
import net.dv8tion.jda.api.Permission

fun CommandEvent.isMissingPermission(permission: Permission) = !hasPermission(permission)
fun CommandEvent.hasPermission(permission: Permission): Boolean {
    if (selfMember == null) return false
    return if (permission.isChannel) {
        selfMember.hasPermission(event.guildChannel, permission)
    } else selfMember.hasPermission(permission)
}

fun CommandEvent.ensurePermission(permission: Permission) {
    if (isMissingPermission(permission)) throw MissingPermissionException(this, permission)
}

fun CommandEvent.missingPermissionMessage(permission: Permission, self: Boolean): String {
    val messageKey = if (self) "core.missing_permission" else "core.missing_user_permission"
    val permissionName = getAny("core.permissions.${permission.translationKey}")
    val contextKey = if (permission.isChannel) "core.channel" else "core.server"
    return getAny(messageKey, permissionName, getAny(contextKey))
}

val Permission.translationKey
    get() = when (this) {
        Permission.CREATE_INSTANT_INVITE -> "create_invite"
        Permission.KICK_MEMBERS -> "kick"
        Permission.BAN_MEMBERS -> "ban"
        Permission.ADMINISTRATOR -> "administrator"
        Permission.MESSAGE_ADD_REACTION -> "add_reactions"
        Permission.VIEW_AUDIT_LOGS -> "view_audit_logs"
        Permission.VIEW_CHANNEL -> "view_channel"
        Permission.MESSAGE_SEND -> "send_messages"
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
        Permission.MODERATE_MEMBERS -> "moderate_members"
        else -> throw AssertionError("Missing permission translation key mapping: $this")
    }
