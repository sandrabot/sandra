/*
 * Copyright 2017-2021 Avery Carroll and Logan Devecka
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

package com.sandrabot.sandra.services

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Service
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity

/**
 * This class updates the bots' presence every minute.
 */
class PresenceService(private val sandra: Sandra) : Service(60) {

    private val activities = arrayListOf(websiteActivity, serverActivity)
    private var lastActivity = -1

    /**
     * When this property is set, this activity
     * will always show and update on [execute].
     */
    var overrideActivity: Activity? = null

    /**
     * This is used to control the bots' online status. When this
     * value is changed, it will only update on the next [execute].
     */
    var status = OnlineStatus.ONLINE

    /**
     * Whether to display the next activity
     * or continue updating the current one.
     */
    var isCycling = true

    override fun execute() = updateAll()

    fun update(shard: JDA) {
        shard.presence.setPresence(status, buildActivity(true))
    }

    fun updateAll() {
        val activity = buildActivity()
        sandra.shards.shardCache.forEachUnordered {
            if (it.status == JDA.Status.CONNECTED) {
                it.presence.setPresence(status, activity)
            }
        }
    }

    fun setDevelopment() {
        overrideActivity = developmentActivity
        status = OnlineStatus.DO_NOT_DISTURB
    }

    private fun buildActivity(useFirst: Boolean = false): Activity {
        val template = when {
            overrideActivity != null -> overrideActivity
            useFirst -> activities[0]
            isCycling -> {
                if (lastActivity == activities.lastIndex) lastActivity = 0 else lastActivity++
                activities[lastActivity]
            }
            else -> sandra.shards.shardCache.first().presence.activity
        } ?: serverActivity
        return Activity.of(template.type, parseActivity(template.name), template.url)
    }

    private fun parseActivity(name: String): String {
        return name.replace("{servers}", "%,d".format(sandra.shards.guildCache.size()))
    }

    companion object {
        private val developmentActivity = Activity.streaming("sandrabot.com", Constants.TWITCH)
        private val serverActivity = Activity.watching("{servers} servers | /help")
        private val websiteActivity = Activity.playing("sandrabot.com | /help")
    }

}
