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
 * Stores Sandra-specific properties and settings for users.
 */
@Serializable
class UserConfig(override val id: Long) : ExperienceConfig() {

    var cash: Long = 0

    @SerialName("daily_last")
    var dailyLast: Long = 0
    @SerialName("daily_streak")
    var dailyStreak: Int = 0
    @SerialName("daily_longest")
    var dailyLongestStreak: Int = 0

    @SerialName("rep")
    var reputation: Long = 0
    @SerialName("rep_last")
    var reputationLast: Long = 0

    @SerialName("last_username")
    var lastUsername: String? = null

    override fun toString(): String = "UserConfig:$id"

}
