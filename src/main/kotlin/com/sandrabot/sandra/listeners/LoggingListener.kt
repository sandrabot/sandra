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

package com.sandrabot.sandra.listeners

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.entities.LogEventType
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.CoroutineEventListener
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.audit.AuditLogEntry
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import kotlin.time.Duration.Companion.milliseconds

class LoggingListener(val sandra: Sandra) : CoroutineEventListener {

    override suspend fun onEvent(event: GenericEvent) {
        // detect what type of event this is, and whether we should act upon it
        when (event) {
            // these are all the guild events we'll listen for
            is GuildMemberJoinEvent -> onGuildMemberJoin(event)
            is GuildMemberRemoveEvent -> onGuildMemberRemove(event)
        }
    }

    private suspend fun sendEvent(
        guild: Guild, eventType: LogEventType, actionType: ActionType?, messageProvider: (AuditLogEntry?) -> String,
    ) {
        val guildConfig = sandra.config.getGuild(guild.idLong)
        // only continue if the logging feature is actually enabled
        if (!guildConfig.loggingEnabled) return
        // count the amount of channels subscribed to this event
        val channels = guildConfig.channels.filterValues {
            eventType in it.enabledLogEvents || LogEventType.ALL in it.enabledLogEvents
        }
        // stop here if nobody is actually subscribed
        if (channels.isEmpty()) return
        // always make sure we have a self member loaded to check permissions against
        val selfMember = if (guild.isLoaded) guild.selfMember else guild.retrieveMember(guild.jda.selfUser).await()
        // determine if we can provide the audit log entry
        val auditEntry = if (actionType != null && selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            delay(150.milliseconds) // race condition, allow discord time to generate the audit log entry
            guild.retrieveAuditLogs().type(actionType).limit(1).await().firstOrNull()
        } else null
        // generate the message content for this log event
        val content = messageProvider(auditEntry)
        // send the message to each subscriber
        for (channel in channels) {
            val guildChannel = guild.getGuildChannelById(channel.key) ?: continue
            // verify messages can be sent to this channel
            if (guildChannel !is GuildMessageChannel) continue
            // ensure that we have permission to view and send messages in this channel
            if (!selfMember.hasPermission(guildChannel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)) continue
            // finally send the message to the event subscriber
            guildChannel.sendMessage(content).queue()
        }
    }

    // TODO User Events

    private suspend fun logMemberEvent(event: GenericGuildEvent, message: (AuditLogEntry?) -> String) =
        sendEvent(event.guild, LogEventType.MEMBER, actionType = null, message)

    private suspend fun onGuildMemberJoin(event: GuildMemberJoinEvent) = logMemberEvent(event) { entry ->
        TODO()
    }

    private suspend fun onGuildMemberRemove(event: GuildMemberRemoveEvent) = logMemberEvent(event) { entry ->
        TODO()
    }

}
