/*
 * Copyright 2026 Avery Carroll, Logan Devecka, and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandrabot.sandra.listeners

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Emojis
import com.sandrabot.sandra.constants.EventType
import com.sandrabot.sandra.constants.FeatureFlag
import com.sandrabot.sandra.entities.LocaleContext
import com.sandrabot.sandra.utils.format
import com.sandrabot.sandra.utils.isFeatureRestricted
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.messages.MessageCreate
import io.ktor.util.date.*
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
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateAvatarEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateFlagsEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateSecurityIncidentActionsEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateSecurityIncidentDetectionsEvent
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.message.poll.MessagePollVoteAddEvent
import net.dv8tion.jda.api.events.message.poll.MessagePollVoteRemoveEvent
import net.dv8tion.jda.api.events.sticker.GuildStickerAddedEvent
import net.dv8tion.jda.api.events.sticker.GuildStickerRemovedEvent
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateDescriptionEvent
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateNameEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageCreateData
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

            // feature: security incident logging
            is GuildUpdateSecurityIncidentActionsEvent -> onUpdateSecurityActions(event)
            is GuildUpdateSecurityIncidentDetectionsEvent -> onUpdateSecurityDetections(event)

            // feature: emoji creation and edit history
            is EmojiAddedEvent -> onEmojiAdded(event)
            is EmojiRemovedEvent -> onEmojiRemoved(event)
            is EmojiUpdateNameEvent -> onEmojiUpdateName(event)

            // feature: sticker creation and updates
            is GuildStickerAddedEvent -> onGuildStickerAdded(event)
            is GuildStickerRemovedEvent -> onGuildStickerRemoved(event)
            is GuildStickerUpdateNameEvent -> onGuildStickerUpdateName(event)
            is GuildStickerUpdateDescriptionEvent -> onGuildStickerUpdateDescription(event)

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
            is GuildMemberUpdateFlagsEvent -> onGuildMemberUpdateFlags(event)
            is GuildMemberUpdateNicknameEvent -> onGuildMemberUpdateNickname(event)
            is GuildMemberUpdateTimeOutEvent -> onGuildMemberUpdateTimeOut(event)

            // feature: deleted messages and edit history
            is MessageBulkDeleteEvent -> onMessageBulkDelete(event)
            is MessageDeleteEvent -> onMessageDelete(event)
            is MessageUpdateEvent -> onMessageUpdate(event)

            // feature: poll announcements and vote history
            is MessagePollVoteAddEvent -> onMessagePollVoteAdd(event)
            is MessagePollVoteRemoveEvent -> onMessagePollVoteRemove(event)
        }
    }

    private suspend fun sendEvent(
        guild: Guild, eventType: EventType, actionType: ActionType?, provider: (AuditLogEntry?) -> MessageCreateData,
    ) {
        // only continue if the logging feature is actually enabled
        val config = sandra.config[guild].takeIf { it.loggingEnabled } ?: return
        // consult with the access manager to determine if this feature is available
        if (isFeatureRestricted(sandra, guild.idLong, FeatureFlag.LOGGING)) return
        // filter channels that are subscribed to this event
        val channels = config.channels.filterValues {
            eventType in it.loggingEventsEnabled || EventType.ALL in it.loggingEventsEnabled
        }.takeUnless { it.isEmpty() } ?: return // we can stop here if nobody is actually subscribed
        // determine if we can provide the audit log entry
        val auditEntry = if (actionType != null && guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            delay(200.milliseconds) // race condition, allow discord time to generate the audit log entry
            try { // attempt to retrieve the latest audit log entry for this action type
                guild.retrieveAuditLogs().type(actionType).limit(1).await().firstOrNull()?.takeUnless {
                    // only accept the latest entry if it was created within the last second
                    getTimeMillis() - it.timeCreated.toInstant().toEpochMilli() > 1_000
                }
            } catch (_: ErrorResponseException) {
                null // nothing we can really do if this fails
            }
        } else null
        // generate the message content for this log event
        val message = provider(auditEntry)
        // send the message to each subscriber
        for (id in channels.keys) {
            // remove stale channel data if the channel doesn't exist anymore
            val channel = guild.getGuildChannelById(id) ?: config.channels.remove(id)
            // verify that messages can be sent to this channel
            if (channel !is GuildMessageChannel || !channel.canTalk()) continue
            channel.sendMessage(message).setAllowedMentions(emptySet()).queue(null, ERROR_HANDLER)
        }
    }

    private suspend fun sendEventMessage(
        guild: Guild, eventType: EventType, actionType: ActionType?, emoji: String? = null,
        provider: (AuditLogEntry?, LocaleContext) -> String,
    ) = sendEvent(guild, eventType, actionType) { entry ->
        val now = getTimeMillis() / 1000
        val content = provider(entry, LocaleContext(guild.locale, "logging"))
        MessageCreate("${emoji ?: eventType.emoji} <t:$now:T> <t:$now:R> $content")
    }

    private suspend fun onAutoModExecution(event: AutoModExecutionEvent) {}
    private suspend fun onAutoModRuleCreate(event: AutoModRuleCreateEvent) {}
    private suspend fun onAutoModRuleDelete(event: AutoModRuleDeleteEvent) {}
    private suspend fun onAutoModRuleUpdate(event: AutoModRuleUpdateEvent) {}

    private suspend fun onUpdateSecurityActions(event: GuildUpdateSecurityIncidentActionsEvent) =
        sendEventMessage(event.guild, EventType.SECURITY, null) { _, context ->
            val oldValues = mapOf(
                "invites" to event.oldValue?.invitesDisabledUntil,
                "direct_messages" to event.oldValue?.directMessagesDisabledUntil,
            ).filterValues { it != null }
            val newValues = mapOf(
                "invites" to event.newValue?.invitesDisabledUntil,
                "direct_messages" to event.newValue?.directMessagesDisabledUntil,
            ).filterValues { it != null }
            if (newValues.size > oldValues.size) {
                val actions = newValues.keys.subtract(oldValues.keys).map { context.getAny("core.phrases.$it") }
                val timestamp = newValues.values.firstOrNull()?.toEpochSecond()
                context["security_action", actions.joinToString("** & **"), "<t:$timestamp:s>"]
            } else {
                val actions = oldValues.keys.subtract(newValues.keys).map { context.getAny("core.phrases.$it") }
                context["security_action_restore", actions.joinToString("** & **")]
            }
        }

    private suspend fun onUpdateSecurityDetections(event: GuildUpdateSecurityIncidentDetectionsEvent) =
        sendEventMessage(event.guild, EventType.SECURITY, null) { _, context ->
            val oldValues = mapOf(
                "raid" to event.oldValue?.timeDetectedRaid,
                "dm_spam" to event.oldValue?.timeDetectedDmSpam,
            ).filterValues { it != null }
            val newValues = mapOf(
                "raid" to event.newValue?.timeDetectedRaid,
                "dm_spam" to event.newValue?.timeDetectedDmSpam,
            ).filterValues { it != null }
            if (newValues.size > oldValues.size) {
                val actions = newValues.keys.subtract(oldValues.keys).map { context.getAny("core.$it") }
                val timestamp = newValues.values.firstOrNull()?.toEpochSecond()
                context["security_detection", actions.joinToString("** & **"), "<t:$timestamp:s>"]
            } else {
                val actions = oldValues.keys.subtract(newValues.keys).map { context.getAny("core.$it") }
                context["security_detection_restore", actions.joinToString("** & **")]
            }
        }

    private suspend fun onEmojiAdded(event: EmojiAddedEvent) =
        sendEventMessage(event.guild, EventType.EMOJI, ActionType.EMOJI_CREATE) { entry, context ->
            context["emoji_create", entry?.user?.asMention, event.emoji.asMention, event.emoji.name]
        }

    private suspend fun onEmojiRemoved(event: EmojiRemovedEvent) =
        sendEventMessage(event.guild, EventType.EMOJI, ActionType.EMOJI_DELETE) { entry, context ->
            context["emoji_delete", entry?.user?.asMention, event.emoji.name]
        }

    private suspend fun onEmojiUpdateName(event: EmojiUpdateNameEvent) =
        sendEventMessage(event.guild, EventType.EMOJI, ActionType.EMOJI_UPDATE) { entry, context ->
            context["emoji_rename", entry?.user?.asMention, event.emoji.asMention, event.newName, event.oldName]
        }

    private suspend fun onGuildStickerAdded(event: GuildStickerAddedEvent) =
        sendEventMessage(event.guild, EventType.STICKER, ActionType.STICKER_CREATE) { entry, context ->
            context["sticker_create", entry?.user?.asMention, event.sticker.name]
        }

    private suspend fun onGuildStickerRemoved(event: GuildStickerRemovedEvent) =
        sendEventMessage(event.guild, EventType.STICKER, ActionType.STICKER_DELETE) { entry, context ->
            context["sticker_delete", entry?.user?.asMention, event.sticker.name]
        }

    private suspend fun onGuildStickerUpdateName(event: GuildStickerUpdateNameEvent) =
        sendEventMessage(event.guild, EventType.STICKER, ActionType.STICKER_UPDATE) { entry, context ->
            context["sticker_rename", entry?.user?.asMention, event.newValue, event.oldValue]
        }

    private suspend fun onGuildStickerUpdateDescription(event: GuildStickerUpdateDescriptionEvent) =
        sendEventMessage(event.guild, EventType.STICKER, ActionType.STICKER_UPDATE, provider = { entry, context ->
            context["sticker_description", entry?.user?.asMention, event.sticker.name]
        })

    private suspend fun onGuildInviteCreate(event: GuildInviteCreateEvent) =
        sendEventMessage(event.guild, EventType.INVITE, ActionType.INVITE_CREATE) { entry, context ->
            val expirations = mutableListOf<String>()
            if (event.invite.maxAge > 0) expirations += "<t:${event.invite.timeCreated.toEpochSecond() + event.invite.maxAge}:R>"
            if (event.invite.maxUses > 0) expirations += context["invite_uses", event.invite.maxUses.format()]
            if (expirations.isEmpty()) expirations += "**${context.getAny("core.phrases.never")}**"
            context["invite_create", entry?.user?.asMention, event.invite.code, event.channel.asMention, expirations.joinToString()]
        }

    private suspend fun onGuildInviteDelete(event: GuildInviteDeleteEvent) =
        sendEventMessage(event.guild, EventType.INVITE, ActionType.INVITE_DELETE, Emojis.UNINVITE) { entry, context ->
            context["invite_delete", entry?.user?.asMention, event.code, event.channel.asMention]
        }

    private suspend fun onGuildBan(event: GuildBanEvent) =
        sendEventMessage(event.guild, EventType.BAN, ActionType.BAN) { entry, context ->
            context["ban", event.user.asMention, event.user.id, entry?.user?.asMention, entry?.reason]
        }

    private suspend fun onGuildUnban(event: GuildUnbanEvent) =
        sendEventMessage(event.guild, EventType.BAN, ActionType.UNBAN) { entry, context ->
            context["unban", event.user.asMention, event.user.id, entry?.user?.asMention, entry?.reason]
        }

    private suspend fun onGuildMemberJoin(event: GuildMemberJoinEvent) {}
    private suspend fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {}
    private suspend fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {}
    private suspend fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {}
    private suspend fun onGuildMemberUpdateAvatar(event: GuildMemberUpdateAvatarEvent) {}
    private suspend fun onGuildMemberUpdateFlags(event: GuildMemberUpdateFlagsEvent) {}
    private suspend fun onGuildMemberUpdateNickname(event: GuildMemberUpdateNicknameEvent) {}
    private suspend fun onGuildMemberUpdateTimeOut(event: GuildMemberUpdateTimeOutEvent) {}

    private suspend fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {}
    private suspend fun onMessageDelete(event: MessageDeleteEvent) {}
    private suspend fun onMessageUpdate(event: MessageUpdateEvent) {}

    private suspend fun onMessagePollVoteAdd(event: MessagePollVoteAddEvent) {}
    private suspend fun onMessagePollVoteRemove(event: MessagePollVoteRemoveEvent) {}

    private companion object {
        private val ERROR_HANDLER = ErrorHandler().ignore(
            ErrorResponse.MISSING_ACCESS, ErrorResponse.MISSING_PERMISSIONS, ErrorResponse.UNKNOWN_CHANNEL
        )
    }

}
