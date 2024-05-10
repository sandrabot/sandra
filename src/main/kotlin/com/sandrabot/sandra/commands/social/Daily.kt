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

package com.sandrabot.sandra.commands.social

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.canDaily
import com.sandrabot.sandra.utils.computeDailyReward
import com.sandrabot.sandra.utils.format
import com.sandrabot.sandra.utils.updateDailyStreak
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
class Daily : Command(arguments = "[user]") {

    override suspend fun execute(event: CommandEvent) {

        // users may only redeem dailies once every 20 hours, unless they're a developer
        if (!event.userConfig.canDaily() && !event.isOwner) {
            val nextDaily = event.userConfig.dailyLast + 72_000_000 // 20 hours
            val remaining = ((nextDaily - System.currentTimeMillis()) / 1_000).seconds.format()
            event.replyEmote(event.get("cooldown", remaining), Emotes.TIME).setEphemeral(true).queue()
            return
        }

        // allow the user to donate their reward to another account
        val targetUser = event.arguments.user() ?: event.user
        // prevent bots from receiving dailies and creating data profiles
        if (targetUser.isBot || targetUser.isSystem) {
            event.replyError(event.get("no_bots")).setEphemeral(true).queue()
            return
        }

        // update the user's daily streak and cooldown
        val lastStreak = event.userConfig.dailyStreak
        event.userConfig.updateDailyStreak()
        // calculate the daily reward based on the updated streak
        val amount = event.userConfig.computeDailyReward()
        // deposit the adjusted amount into the target user's account
        event.sandra.config[targetUser].cash += amount

        // figure out which message to display and format it accordingly
        val streakInfo = when (event.userConfig.dailyStreak) {
            0 -> if (lastStreak > 0) {
                event.get("streak.ended", lastStreak.format(), event.userConfig.dailyLongestStreak.format())
            } else event.get("streak.hint")

            1 -> event.get("streak.started")

            else -> {
                val nextDailySeconds = (event.userConfig.dailyLast + 72_000_000) / 1_000
                event.get("streak.continued", event.userConfig.dailyStreak.format(), "<t:$nextDailySeconds:t>")
            }
        }

        val context = if (event.user == targetUser) "self" else "other"
        val header = event.get(context, Emotes.CASH, amount.format(), targetUser)
        event.reply("$header\n$streakInfo").queue()

    }
}
