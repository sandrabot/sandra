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

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.LanguageContext
import com.sandrabot.sandra.entities.blocklist.BlocklistEntry
import com.sandrabot.sandra.entities.blocklist.FeatureType
import com.sandrabot.sandra.entities.blocklist.TargetType
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.managers.BlocklistManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageChannel
import org.slf4j.LoggerFactory

fun checkCommandBlocklist(event: CommandEvent): Boolean {
    val guildId = if (event.isFromGuild) null else event.guild.idLong
    return checkBlocklist(event.sandra, event.channel, event.author.idLong, guildId, FeatureType.COMMANDS)
}

fun checkBlocklist(
    sandra: Sandra, channel: MessageChannel, userId: Long, guildId: Long?, featureType: FeatureType
): Boolean {
    // This is the easiest way to process different contexts at the same time
    val entry = arrayOf(userId, guildId).filterNotNull().mapNotNull {
        sandra.blocklist.getEntry(it)
    }.find { it.isFeatureBlocked(featureType) } ?: return false
    // Always make sure that the context has been notified
    if (!entry.isNotified(featureType)) blocklistNotify(sandra, channel, userId, guildId, featureType, entry)
    return true
}

fun blocklistNotify(
    sandra: Sandra, channel: MessageChannel, userId: Long,
    guildId: Long?, featureType: FeatureType, entry: BlocklistEntry
) {
    val (entryName, locale) = if (entry.targetType == TargetType.GUILD) {
        // The guildId should only be null if the context is not from a guild
        val guild = sandra.shards.getGuildById(guildId ?: return) ?: return
        // If this channel is in a guild, make sure we have permissions to even continue
        if (!guild.selfMember.hasPermission(Permission.MESSAGE_WRITE)) return
        guild.name.sanitize() to sandra.config.getGuild(guildId).locale
    } else {
        val user = sandra.shards.getUserById(userId) ?: return
        user.name.sanitize() to sandra.config.getUser(userId).locale
    }
    val reason = entry.getReason(featureType)
    val blockedMessage = Unicode.CROSS_MARK + Unicode.VERTICAL_LINE +
            LanguageContext(sandra, locale).translate("general.blocked", entryName, reason)
    channel.sendMessage(blockedMessage).queue {
        entry.recordNotify(featureType, channel.idLong, it.idLong)
        LoggerFactory.getLogger(BlocklistManager::class.java).info(
            """This context has been notified of feature $featureType and reason "$reason" with message ${it.id} in channel ${channel.id}"""
        )
    }
}
