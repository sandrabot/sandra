/*
 * Copyright 2017-2020 Avery Clifton and Logan Devecka
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

package com.sandrabot.sandra.managers

import com.beust.klaxon.Klaxon
import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.RedisPrefix
import com.sandrabot.sandra.entities.Cooldown
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.asReaction
import com.sandrabot.sandra.utils.hasPermissions
import net.dv8tion.jda.api.Permission

class CooldownManager(private val sandra: Sandra) {

    private val cooldowns = mutableMapOf<String, Cooldown>()

    val size: Int
        get() = cooldowns.size

    init {
        val data = sandra.redis.get(RedisPrefix.SETTING + "cooldowns") ?: "[]"
        Klaxon().parseArray<Cooldown>(data)!!.forEach { cooldowns[it.cooldownKey] = it }
    }

    fun applyCooldown(event: CommandEvent): Boolean {
        val cooldown = synchronized(cooldowns) {
            cooldowns.compute(event.cooldownKey) { key, value ->
                if (value == null || value.isExpired) {
                    Cooldown(key, event.command.cooldown)
                } else value
            } as Cooldown
        }

        when (++cooldown.attempts) {
            // Don't block the first attempt
            1 -> return false
            // Attempt to add a timer reaction on their second attempt
            2 -> if (hasPermissions(event, Permission.MESSAGE_ADD_REACTION,
                            Permission.MESSAGE_HISTORY, Permission.MESSAGE_EXT_EMOJI)) {
                event.message.addReaction(Emotes.TIME.asReaction()).queue()
            }
        }

        return true
    }

    fun clean() {
        synchronized(cooldowns) {
            cooldowns.filterValues { it.isExpired }.forEach { (key, _) ->
                cooldowns.remove(key)
            }
        }
    }

    fun shutdown() {
        val data = Klaxon().toJsonString(cooldowns.values)
        sandra.redis.set(RedisPrefix.SETTING + "cooldowns", data)
    }

}
