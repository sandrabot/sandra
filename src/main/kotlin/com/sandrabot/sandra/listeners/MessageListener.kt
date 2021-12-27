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
        // TODO Feature: Sever Experience
        // TODO Feature: Global Experience
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
