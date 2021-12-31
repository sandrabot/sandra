/*
 * Copyright 2017-2022 Avery Carroll and Logan Devecka
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
import com.sandrabot.sandra.entities.blocklist.FeatureType
import com.sandrabot.sandra.utils.awardExperience
import com.sandrabot.sandra.utils.canExperience
import com.sandrabot.sandra.utils.checkBlocklist
import com.sandrabot.sandra.utils.randomExperience
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import org.slf4j.LoggerFactory

/**
 * Class for handling features related to message content.
 */
class MessageListener(private val sandra: Sandra) {

    @Suppress("unused")
    fun onMessageReceived(event: MessageReceivedEvent) {

        sandra.statistics.incrementMessageCount()

        // Ignore messages from bots and webhooks. We don't check the
        // blocklist here so users aren't exempt from auto moderation
        if (event.author.isBot || event.isWebhookMessage) return

        // We can safely ignore any system messages
        if (event.message.type != MessageType.DEFAULT) return

        // If this message was sent from a guild, handle any moderation features
        if (event.isFromGuild) {
            sandra.messages.put(event.message)
            if (handleAntiAdvertising(event)) return
        }

        // Handle specific features for where the message was sent
        if (event.isFromGuild) handleGuildMessage(event) else handlePrivateMessage(event)

    }

    @Suppress("unused")
    fun onMessageUpdate(event: MessageUpdateEvent) {
        if (!event.isFromGuild) return
        val message = event.message
        // Once again ignore messages from bots or webhooks
        if (event.author.isBot || message.isWebhookMessage) return
        // TODO Recheck the message for invites and handle anti-advertise
    }

    private fun handleGuildMessage(event: MessageReceivedEvent) {
        val authorId = event.author.idLong
        val guildId = event.guild.idLong

        // Check the blocklist to prevent responding to active contexts
        if (checkBlocklist(sandra, event.channel, authorId, guildId, FeatureType.MESSAGES)) return

        val guildConfig = sandra.config.getGuild(guildId)
        // The member is never null since we ignore webhooks
        val memberConfig = guildConfig.getMember(authorId)

        // TODO Feature: AFK Messages

        // Feature: Server Experience
        // TODO Channel Config: Is Experience Allowed
        if (guildConfig.experienceEnabled && memberConfig.canExperience()) {
            // Award a random amount of experience between 15 and 25
            // TODO Feature: Experience Multipliers
            if (memberConfig.awardExperience(randomExperience())) {
                // TODO Feature: Level Up Notifications with custom Messages
                // TODO Feature: Level Up Rewards
            }
        }

        // Feature: Global Experience
        // Check to make sure this user is allowed to gain global experience
        if (!checkBlocklist(sandra, event.channel, authorId, guildId, FeatureType.GLOBAL_EXPERIENCE)) {
            val userConfig = sandra.config.getUser(authorId)
            // Check to see if this user can receive experience
            if (userConfig.canExperience()) {
                // Award a random amount of experience between 15 and 25
                if (userConfig.awardExperience(randomExperience())) {
                    // TODO Feature: Global Level Up Notifications and Rewards
                }
            }
        }

        // TODO Feature: Message Replies
    }

    private fun handlePrivateMessage(event: MessageReceivedEvent) {
        // Hi this looks nicer than declaring them individually :)
        val (author, message) = event.author to event.message
        logger.info("Direct Message: ${author.asTag} [${author.id}] | ${message.contentDisplay}")
        message.attachments.forEach {
            logger.info("Direct Message Attachment: ${author.asTag} [${author.id}] | ${it.url}")
        }
    }

    private fun handleAntiAdvertising(event: MessageReceivedEvent): Boolean {
        // TODO Check the message for any foreign invites and delete them
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MessageListener::class.java)
    }

}
