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

package com.sandrabot.sandra.managers

import com.beust.klaxon.Klaxon
import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.RedisPrefix
import com.sandrabot.sandra.entities.Cooldown
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.format
import com.sandrabot.sandra.utils.hasPermissions
import net.dv8tion.jda.api.Permission
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import java.util.concurrent.TimeUnit

class CooldownManager(private val sandra: Sandra) {

    private val cooldowns: ExpiringMap<String, Cooldown> = ExpiringMap.builder()
        .expiration(Constants.DEFAULT_COOLDOWN.toLong(), TimeUnit.MILLISECONDS)
        .expirationPolicy(ExpirationPolicy.CREATED)
        .variableExpiration().build()

    init {
        val data = sandra.redis[RedisPrefix.SETTING + "cooldowns"] ?: "[]"
        Klaxon().parseArray<Cooldown>(data)?.forEach {
            cooldowns[it.cooldownKey] = it
            cooldowns.setExpiration(it.cooldownKey, it.remaining, TimeUnit.MILLISECONDS)
        }
    }

    fun shutdown() {
        val data = Klaxon().toJsonString(cooldowns.values)
        sandra.redis[RedisPrefix.SETTING + "cooldowns"] = data
    }

    fun applyCooldown(event: CommandEvent, duration: Int = event.command.cooldown): Boolean {
        val cooldown = cooldowns[event.cooldownKey] ?: run {
            if (event.command.cooldown == 0) return false
            cooldowns[event.cooldownKey] = Cooldown(event.cooldownKey, duration)
            cooldowns.setExpiration(event.cooldownKey, duration.toLong(), TimeUnit.MILLISECONDS)
            return false
        }
        // Only attempt to notify when exceeding the cooldown the first time
        val hasPermissions = hasPermissions(event, Permission.MESSAGE_WRITE, Permission.MESSAGE_EXT_EMOJI)
        if (++cooldown.attempts == 1 && hasPermissions) {
            val formattedDuration = (cooldown.remaining / 1000.0).format()
            event.replyEmote(event.translate("general.cooldown", formattedDuration), Emotes.TIME, {
                cooldown.isNotified = true
            })
        }
        return true
    }

}
