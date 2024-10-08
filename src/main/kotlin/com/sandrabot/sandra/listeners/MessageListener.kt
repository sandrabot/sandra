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
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.LocaleContext
import com.sandrabot.sandra.entities.blocklist.FeatureType
import com.sandrabot.sandra.utils.*
import dev.minn.jda.ktx.events.CoroutineEventListener
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory

/**
 * Event listener that processes events related to message content.
 */
class MessageListener(private val sandra: Sandra) : CoroutineEventListener {

    override suspend fun onEvent(event: GenericEvent) {
        when (event) {
            is MessageReceivedEvent -> onMessageReceived(event)
        }
    }

    /**
     * Handler for all [MessageReceivedEvent] events.
     * Messages sent by bots, webhooks, or system are ignored.
     */
    private suspend fun onMessageReceived(event: MessageReceivedEvent) {

        // TODO Feature: Internal Metrics
        sandra.statistics.incrementMessageCount()

        // Ignore messages from bots and webhooks. We don't check the
        // blocklist here so that users aren't exempt from auto moderation
        if (event.author.isBot || event.isWebhookMessage) return

        // We can safely ignore any system messages
        if (event.message.type != MessageType.DEFAULT) return

        // If this message was sent from a guild, add it to our cache
        if (event.isFromGuild) sandra.messages.put(event.message)

        // Handle specific features depending on where the message was sent
        if (event.isFromGuild) handleGuildMessage(event) else handlePrivateMessage(event)

    }

    /**
     * Processes all messages received within all guild channels.
     */
    private fun handleGuildMessage(event: MessageReceivedEvent) {
        val authorId = event.author.idLong
        val guildId = event.guild.idLong
        val channelId = event.channel.idLong

        // Check the blocklist to prevent responding in actively blocked contexts
        if (checkBlocklist(sandra, event.channel, authorId, guildId, FeatureType.MESSAGES)) return

        val guildConfig = sandra.config.getGuild(guildId)
        val memberConfig = guildConfig.getMember(authorId)
        val channelConfig = guildConfig.getChannel(channelId)

        val userConfig = sandra.config.getUser(authorId)
        val localeContext = LocaleContext(event.guild, event.author.probableLocale())

        // TODO Feature: AFK Messages

        // Feature: Server Experience
        if (guildConfig.experienceEnabled && memberConfig.canExperience() && channelConfig.experienceEnabled) {
            // Award a random amount of experience between 15 and 25
            // Multiply the amount based on the multiplier configuration
            val multiplier = guildConfig.computeMultiplier(channelConfig)
            if (memberConfig.awardExperience(randomExperience(multiplier))) {
                // Check if the guild has level up notifications enabled
                if (guildConfig.experienceNotifyEnabled) {
                    // Check if the guild has a specific channel for notifications
                    // Otherwise, check if this channel can receive notifications
                    val notifyChannel = if (guildConfig.experienceNotifyChannel != 0L) {
                        event.guild.getGuildChannelById(guildConfig.experienceNotifyChannel)
                    } else if (channelConfig.experienceNotifyEnabled) event.guildChannel else null
                    // Make sure we have the permissions to send messages in the channel
                    if (notifyChannel is GuildMessageChannel && notifyChannel.canTalk()) {
                        // Figure out which template to use and format it with the correct details
                        val notifyTemplate = guildConfig.experienceNotifyTemplate ?: localeContext.getAny(
                            "core.experience_notify", Emotes.LEVEL_UP
                        )
                        // Member will never be null since we always ignore bots and webhooks
                        val formattedTemplate = notifyTemplate.formatTemplate(sandra, event.guild, event.member!!)
                        notifyChannel.sendMessage(formattedTemplate).apply {
                            // make sure that we actually have permission to reply to a message
                            if (event.guild.selfMember.hasPermission(notifyChannel, Permission.MESSAGE_HISTORY)) {
                                // if the notification channel is not where the message was sent, the reference will do nothing
                                setMessageReference(event.message)
                            }
                        }.queue()
                    }
                }
                // TODO Feature: Level Up Rewards
            }
        }

        // Feature: Global Experience
        // Check to make sure this user is allowed to gain global experience
        if (!checkBlocklist(sandra, event.channel, authorId, guildId, FeatureType.GLOBAL_EXPERIENCE)) {
            // Check to see if this user can receive experience
            // Award a random amount of experience between 15 and 25
            if (userConfig.canExperience()) userConfig.awardExperience(randomExperience())
        }

        // TODO Feature: Message Replies
    }

    /**
     * Processes any private messages that the bot receives.
     */
    private fun handlePrivateMessage(event: MessageReceivedEvent) = buildString {
        append("Direct Message: ", event.author.name, " [", event.author.id, "] | ", event.message.contentDisplay)
        with(event.message.attachments) {
            if (isNotEmpty()) forEach { append("\nDirect Message Attachment: ${it.url}") }
        }
    }.let { logger.info(it) }

    private companion object {
        private val logger = LoggerFactory.getLogger(MessageListener::class.java)
    }

}
