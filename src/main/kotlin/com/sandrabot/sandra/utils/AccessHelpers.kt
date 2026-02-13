/*
 * Copyright 2017-2026 Avery Carroll and Logan Devecka
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

import com.sandrabot.sandra.commands.essential.Feedback
import com.sandrabot.sandra.entities.Category
import com.sandrabot.sandra.entities.FeatureFlag
import com.sandrabot.sandra.events.CommandEvent

fun CommandEvent.isAccessRestricted(): Boolean = when {
    command is Feedback -> checkContext(FeatureFlag.FEEDBACK)
    command.category == Category.LASTFM -> checkContext(FeatureFlag.LASTFM)
    command.category == Category.GAME -> checkContext(FeatureFlag.MINIGAMES)
    command.category == Category.SOCIAL -> checkContext(FeatureFlag.SOCIAL)
    else -> false
} || checkContext(FeatureFlag.COMMANDS)

private fun CommandEvent.checkContext(feature: FeatureFlag): Boolean {
    if (sandra.access.isFeatureDisabled(feature)) return true
    val isUserRevoked = sandra.access.isAccessRevoked(user.idLong, feature)
    return if (isFromGuild) isUserRevoked || sandra.access.isAccessRevoked(guild!!.idLong, feature) else isUserRevoked
}
