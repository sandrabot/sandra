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

package com.sandrabot.sandra.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stores Sandra-specific properties and settings for guilds.
 */
@Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
@Serializable
class GuildConfig(override val id: Long) : Configuration() {

    @Serializable(with = ConfigMapTransformer::class)
    val channels = mutableMapOf<Long, ChannelConfig>()

    @Serializable(with = ConfigMapTransformer::class)
    val members = mutableMapOf<Long, MemberConfig>()

    @SerialName("experience")
    var experienceEnabled: Boolean = true
    @SerialName("compounds")
    var experienceCompounds: Boolean = true
    @SerialName("multiplier")
    var experienceMultiplier: Double = 1.0
    @SerialName("notify")
    var experienceNotifyEnabled: Boolean = true
    @SerialName("notify_template")
    var experienceNotifyTemplate: String? = null
    @SerialName("notify_channel")
    var experienceNotifyChannel: Long = 0L

    @SerialName("last_upvote")
    var lastUpvoteEmoji: String? = null
    @SerialName("last_downvote")
    var lastDownvoteEmoji: String? = null

    fun getChannel(id: Long): ChannelConfig = channels.getOrPut(id) { ChannelConfig(id) }
    fun getMember(id: Long): MemberConfig = members.getOrPut(id) { MemberConfig(id) }

    override fun toString(): String = "GuildConfig:$id"

}
