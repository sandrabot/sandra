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
import net.dv8tion.jda.api.events.automod.AutoModExecutionEvent
import net.dv8tion.jda.api.events.automod.AutoModRuleCreateEvent
import net.dv8tion.jda.api.events.automod.AutoModRuleDeleteEvent
import net.dv8tion.jda.api.events.automod.AutoModRuleUpdateEvent
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateNameEvent
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateRolesEvent
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.guild.member.update.*
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventCreateEvent
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventDeleteEvent
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventUserAddEvent
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventUserRemoveEvent
import net.dv8tion.jda.api.events.guild.scheduledevent.update.*
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.message.poll.MessagePollVoteAddEvent
import net.dv8tion.jda.api.events.message.poll.MessagePollVoteRemoveEvent
import net.dv8tion.jda.api.events.sticker.GuildStickerAddedEvent
import net.dv8tion.jda.api.events.sticker.GuildStickerRemovedEvent
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateDescriptionEvent
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateNameEvent
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateTagsEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import kotlin.time.Duration.Companion.milliseconds

class LoggingListener(val sandra: Sandra) : CoroutineEventListener {

    override suspend fun onEvent(event: GenericEvent) {
        // detect what type of event this is, and whether we should act upon it
        when (event) {
            // feature: auto mod logging
            is AutoModExecutionEvent -> onAutoModExecution(event)
            is AutoModRuleCreateEvent -> onAutoModRuleCreate(event)
            is AutoModRuleDeleteEvent -> onAutoModRuleDelete(event)
            is AutoModRuleUpdateEvent -> onAutoModRuleUpdate(event)

            // feature: emoji creation and edit history
            is EmojiAddedEvent -> onEmojiAdded(event)
            is EmojiRemovedEvent -> onEmojiRemoved(event)
            is EmojiUpdateNameEvent -> onEmojiUpdateName(event)
            is EmojiUpdateRolesEvent -> onEmojiUpdateRoles(event)

            // feature: sticker creation and updates
            is GuildStickerAddedEvent -> onGuildStickerAdded(event)
            is GuildStickerRemovedEvent -> onGuildStickerRemoved(event)
            is GuildStickerUpdateNameEvent -> onGuildStickerUpdateName(event)
            is GuildStickerUpdateDescriptionEvent -> onGuildStickerUpdateDescription(event)
            is GuildStickerUpdateTagsEvent -> onGuildStickerUpdateTags(event)

            // feature: invite creation and deletion
            is GuildInviteCreateEvent -> onGuildInviteCreate(event)
            is GuildInviteDeleteEvent -> onGuildInviteDelete(event)

            // feature: log bans and unbans
            is GuildBanEvent -> onGuildBan(event)
            is GuildUnbanEvent -> onGuildUnban(event)

            // feature: member joins, leaves, and updates
            is GuildMemberJoinEvent -> onGuildMemberJoin(event)
            is GuildMemberRemoveEvent -> onGuildMemberRemove(event)
            is GuildMemberRoleAddEvent -> onGuildMemberRoleAdd(event)
            is GuildMemberRoleRemoveEvent -> onGuildMemberRoleRemove(event)
            is GuildMemberUpdateAvatarEvent -> onGuildMemberUpdateAvatar(event)
            is GuildMemberUpdateBoostTimeEvent -> onGuildMemberUpdateBoostTime(event)
            is GuildMemberUpdateFlagsEvent -> onGuildMemberUpdateFlags(event)
            is GuildMemberUpdateNicknameEvent -> onGuildMemberUpdateNickname(event)
            is GuildMemberUpdatePendingEvent -> onGuildMemberUpdatePending(event)
            is GuildMemberUpdateTimeOutEvent -> onGuildMemberUpdateTimeOut(event)

            // feature: event creation and updates
            is ScheduledEventCreateEvent -> onScheduledEventCreate(event)
            is ScheduledEventDeleteEvent -> onScheduledEventDelete(event)
            is ScheduledEventUserAddEvent -> onScheduledEventUserAdd(event)
            is ScheduledEventUserRemoveEvent -> onScheduledEventUserRemove(event)
            is ScheduledEventUpdateNameEvent -> onScheduledEventUpdateName(event)
            is ScheduledEventUpdateDescriptionEvent -> onScheduledEventUpdateDescription(event)
            is ScheduledEventUpdateStartTimeEvent -> onScheduledEventUpdateStartTime(event)
            is ScheduledEventUpdateEndTimeEvent -> onScheduledEventUpdateEndTime(event)
            is ScheduledEventUpdateImageEvent -> onScheduledEventUpdateImage(event)
            is ScheduledEventUpdateLocationEvent -> onScheduledEventUpdateLocation(event)
            is ScheduledEventUpdateStatusEvent -> onScheduledEventUpdateStatus(event)

            // feature: deleted messages and edit history
            is MessageBulkDeleteEvent -> onMessageBulkDelete(event)
            is MessageDeleteEvent -> onMessageDelete(event)
            is MessageUpdateEvent -> onMessageUpdate(event)

            // feature: poll announcements and vote history
            is MessagePollVoteAddEvent -> onMessagePollVoteAdd(event)
            is MessagePollVoteRemoveEvent -> onMessagePollVoteRemove(event)

            // feature: username changes and updates
            is UserUpdateAvatarEvent -> onUserUpdateAvatar(event)
            is UserUpdateGlobalNameEvent -> onUserUpdateGlobalName(event)
            is UserUpdateNameEvent -> onUserUpdateName(event)
        }
    }

    private suspend fun sendEvent(
        guild: Guild, eventType: LogEventType, actionType: ActionType?, messageProvider: (AuditLogEntry?) -> String,
    ) {
        // only continue if the logging feature is actually enabled
        val config = sandra.config[guild].takeIf { it.loggingEnabled } ?: return
        // count the amount of channels subscribed to this event
        val channels = config.channels.filterValues {
            eventType in it.enabledLogEvents || LogEventType.ALL in it.enabledLogEvents
        }.takeUnless { it.isEmpty() } ?: return // don't continue if nobody is actually subscribed
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
        for (id in channels.keys) {
            val channel = guild.getGuildChannelById(id) ?: continue
            // verify that messages can be sent to this channel
            if (channel !is GuildMessageChannel) continue
            // ensure that we have permission to view and send messages in this channel
            if (!selfMember.hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)) continue
            // finally send the message to the event subscriber
            channel.sendMessage(content).queue(null, ERROR_HANDLER)
        }
    }

    private suspend fun sendUserEvent() {
        TODO("Not yet implemented")
    }

    private suspend fun onAutoModExecution(event: AutoModExecutionEvent) {}
    private suspend fun onAutoModRuleCreate(event: AutoModRuleCreateEvent) {}
    private suspend fun onAutoModRuleDelete(event: AutoModRuleDeleteEvent) {}
    private suspend fun onAutoModRuleUpdate(event: AutoModRuleUpdateEvent) {}

    private suspend fun onEmojiAdded(event: EmojiAddedEvent) {}
    private suspend fun onEmojiRemoved(event: EmojiRemovedEvent) {}
    private suspend fun onEmojiUpdateName(event: EmojiUpdateNameEvent) {}
    private suspend fun onEmojiUpdateRoles(event: EmojiUpdateRolesEvent) {}

    private suspend fun onGuildStickerAdded(event: GuildStickerAddedEvent) {}
    private suspend fun onGuildStickerRemoved(event: GuildStickerRemovedEvent) {}
    private suspend fun onGuildStickerUpdateName(event: GuildStickerUpdateNameEvent) {}
    private suspend fun onGuildStickerUpdateDescription(event: GuildStickerUpdateDescriptionEvent) {}
    private suspend fun onGuildStickerUpdateTags(event: GuildStickerUpdateTagsEvent) {}

    private suspend fun onGuildInviteCreate(event: GuildInviteCreateEvent) {}
    private suspend fun onGuildInviteDelete(event: GuildInviteDeleteEvent) {}

    private suspend fun onGuildBan(event: GuildBanEvent) {}
    private suspend fun onGuildUnban(event: GuildUnbanEvent) {}

    private suspend fun onGuildMemberJoin(event: GuildMemberJoinEvent) {}
    private suspend fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {}
    private suspend fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {}
    private suspend fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {}
    private suspend fun onGuildMemberUpdateAvatar(event: GuildMemberUpdateAvatarEvent) {}
    private suspend fun onGuildMemberUpdateBoostTime(event: GuildMemberUpdateBoostTimeEvent) {}
    private suspend fun onGuildMemberUpdateFlags(event: GuildMemberUpdateFlagsEvent) {}
    private suspend fun onGuildMemberUpdateNickname(event: GuildMemberUpdateNicknameEvent) {}
    private suspend fun onGuildMemberUpdatePending(event: GuildMemberUpdatePendingEvent) {}
    private suspend fun onGuildMemberUpdateTimeOut(event: GuildMemberUpdateTimeOutEvent) {}

    private suspend fun onScheduledEventCreate(event: ScheduledEventCreateEvent) {}
    private suspend fun onScheduledEventDelete(event: ScheduledEventDeleteEvent) {}
    private suspend fun onScheduledEventUserAdd(event: ScheduledEventUserAddEvent) {}
    private suspend fun onScheduledEventUserRemove(event: ScheduledEventUserRemoveEvent) {}
    private suspend fun onScheduledEventUpdateName(event: ScheduledEventUpdateNameEvent) {}
    private suspend fun onScheduledEventUpdateDescription(event: ScheduledEventUpdateDescriptionEvent) {}
    private suspend fun onScheduledEventUpdateStartTime(event: ScheduledEventUpdateStartTimeEvent) {}
    private suspend fun onScheduledEventUpdateEndTime(event: ScheduledEventUpdateEndTimeEvent) {}
    private suspend fun onScheduledEventUpdateImage(event: ScheduledEventUpdateImageEvent) {}
    private suspend fun onScheduledEventUpdateLocation(event: ScheduledEventUpdateLocationEvent) {}
    private suspend fun onScheduledEventUpdateStatus(event: ScheduledEventUpdateStatusEvent) {}

    private suspend fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {}
    private suspend fun onMessageDelete(event: MessageDeleteEvent) {}
    private suspend fun onMessageUpdate(event: MessageUpdateEvent) {}

    private suspend fun onMessagePollVoteAdd(event: MessagePollVoteAddEvent) {}
    private suspend fun onMessagePollVoteRemove(event: MessagePollVoteRemoveEvent) {}

    private suspend fun onUserUpdateAvatar(event: UserUpdateAvatarEvent) {}
    private suspend fun onUserUpdateGlobalName(event: UserUpdateGlobalNameEvent) {}
    private suspend fun onUserUpdateName(event: UserUpdateNameEvent) {}

    private companion object {
        private val ERROR_HANDLER = ErrorHandler().ignore(
            ErrorResponse.MISSING_ACCESS, ErrorResponse.MISSING_PERMISSIONS, ErrorResponse.UNKNOWN_CHANNEL
        )
    }

}
