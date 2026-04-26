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

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.commands.essential.Feedback
import com.sandrabot.sandra.constants.Category
import com.sandrabot.sandra.constants.FeatureFlag
import com.sandrabot.sandra.events.CommandEvent

fun isFeatureAllowed(sandra: Sandra, user: Long, feature: FeatureFlag) = !isFeatureRestricted(sandra, user, feature)
fun isFeatureRestricted(sandra: Sandra, id: Long, feature: FeatureFlag): Boolean {
    return sandra.access.isFeatureDisabled(feature) || sandra.access.isAccessRevoked(id, feature)
}

fun isContextAllowed(
    sandra: Sandra, authorId: Long, guildId: Long, feature: FeatureFlag,
) = !isContextRestricted(sandra, authorId, guildId, feature)

fun isContextRestricted(sandra: Sandra, user: Long, guild: Long?, feature: FeatureFlag): Boolean {
    val isUserRevoked = isFeatureRestricted(sandra, user, feature)
    return if (guild != null) isUserRevoked || isFeatureRestricted(sandra, guild, feature) else isUserRevoked
}

fun CommandEvent.isAccessRestricted(): Boolean = when {
    command is Feedback -> checkContext(FeatureFlag.FEEDBACK)
    command.category == Category.LASTFM -> checkContext(FeatureFlag.LASTFM)
    command.category == Category.GAME -> checkContext(FeatureFlag.MINIGAMES)
    command.category == Category.SOCIAL -> checkContext(FeatureFlag.SOCIAL)
    else -> false
} || checkContext(FeatureFlag.COMMANDS)

private fun CommandEvent.checkContext(feature: FeatureFlag) =
    isContextRestricted(sandra, user.idLong, guild?.idLong, feature)
