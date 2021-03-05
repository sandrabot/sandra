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

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.blocklist.FeatureType
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.managers.BlocklistManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(BlocklistManager::class.java)

fun checkBlocklist(event: CommandEvent, featureType: FeatureType): Boolean {
    val entry = arrayOf(event.author.idLong, event.guild.idLong).mapNotNull {
        event.sandra.blocklist.getEntry(it)
    }.find { it.isFeatureBlocked(featureType) } ?: return false
    if (!entry.isNotified(featureType) && hasPermission(event, Permission.MESSAGE_WRITE)) {
        val name = (if (entry.targetId == event.author.idLong) event.author.name else event.guild.name).sanitize()
        val reason = entry.getReason(featureType)
        val message = event.translate("general.blocked", name, reason)
        val consumer: (Message) -> Unit = {
            logger.info("This context has been notified of feature $featureType and reason \"$reason\" with message ${it.idLong}")
            entry.recordNotify(featureType, event.channel.idLong, it.idLong)
        }
        if (missingPermission(event, Permission.MESSAGE_EXT_EMOJI)) {
            event.reply("${Unicode.CROSS_MARK} $message", consumer)
        } else event.replyError(message, consumer)
    }
    return true
}
