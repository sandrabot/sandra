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

package com.sandrabot.sandra.config

import com.sandrabot.sandra.entities.Privilege
import com.sandrabot.sandra.utils.isAllowed
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 * Stores Sandra-specific properties and settings for channels within guilds.
 */
@Serializable
class ChannelConfig(override val id: Long) : Configuration() {

    val experiencePrivileges = mutableListOf<Privilege>()
    var experienceAllowed: Boolean = true
    var experienceMultiplier: Double = 1.0

    fun isExperienceAllowed(event: MessageReceivedEvent): Boolean =
        experienceAllowed && experiencePrivileges.isAllowed(event)

    override fun toString(): String = "ChannelConfig:$id"

}
