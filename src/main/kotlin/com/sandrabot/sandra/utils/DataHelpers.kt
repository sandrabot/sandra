/*
 * Copyright 2017-2024 Avery Carroll and Logan Devecka
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

import com.sandrabot.sandra.config.Configuration
import com.sandrabot.sandra.config.GuildConfig
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.Guild

private val json = Json {
    encodeDefaults = true
    prettyPrint = true
}

@Suppress("unused")
fun Configuration.toData() = json.encodeToString(this)

fun GuildConfig.cleanRoleData(guild: Guild) {
    val currentRoles = guild.roleCache.map { it.idLong }
    if (defaultRoles.isNotEmpty()) defaultRoles.removeIf { it !in currentRoles }
    if (revokedRoles.isNotEmpty()) revokedRoles.removeIf { it !in currentRoles }
    if (defaultBotRole != 0L && defaultBotRole !in currentRoles) defaultBotRole = 0L
    members.values.forEach { config -> config.savedRoles.removeIf { it !in currentRoles } }
}
