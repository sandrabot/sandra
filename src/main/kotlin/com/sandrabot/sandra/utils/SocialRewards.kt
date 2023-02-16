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

import com.sandrabot.sandra.config.ChannelConfig
import com.sandrabot.sandra.config.ExperienceConfig
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.UserConfig
import kotlin.math.roundToInt

val experienceLevelGoals: List<Int> = run {
    var (increment, step) = 100 to 55
    generateSequence {
        val goal = increment.also {
            increment += step
            step += 10
        }
        if (goal < 0) null else goal
    }.toList()
}

fun randomExperience(multiplier: Double = 1.0): Int = ((15..25).random() * multiplier).roundToInt()

fun GuildConfig.computeMultiplier(channel: ChannelConfig? = null): Double = when {
    channel == null -> experienceMultiplier
    experienceCompounds -> experienceMultiplier * channel.experienceMultiplier
    channel.experienceMultiplier != 1.0 -> channel.experienceMultiplier
    else -> experienceMultiplier
}

fun ExperienceConfig.canExperience() = System.currentTimeMillis() >= experienceLast + 60_000 // 1 minute

fun ExperienceConfig.awardExperience(amount: Int): Boolean {
    experience += amount // Add the new experience to the old value
    experienceLast = System.currentTimeMillis() // Update the experience timer
    val goal = experienceLevelGoals[level] // Get the current experience goal
    // Check if the new experience amount reached this goal
    return if (experience >= goal) {
        experience -= goal // Reset the experience counter and keep any rollover
        level += 1 // Increase the current level
        true // Signify to the caller there was a level up
    } else false
}

fun UserConfig.canReputation() = System.currentTimeMillis() >= reputationLast + 72_000_000 // 20 hours
