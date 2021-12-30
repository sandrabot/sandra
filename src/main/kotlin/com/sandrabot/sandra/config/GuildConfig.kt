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

import com.sandrabot.sandra.entities.Locale
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.Member

/**
 * Stores Sandra-specific properties and settings for guilds.
 */
@Serializable
class GuildConfig(override val id: Long) : Configuration() {

    val members = mutableListOf<MemberConfig>()
    var locale: Locale = Locale.DEFAULT

    fun getMember(member: Member): MemberConfig = getMember(member.idLong)
    fun getMember(id: Long): MemberConfig = synchronized(members) {
        members.find { it.id == id } ?: MemberConfig(id).also { members += it }
    }

    override fun toString(): String = "GuildConfig:$id"

}
