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

package com.sandrabot.sandra.listeners

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.getPrefixUsed
import com.sandrabot.sandra.utils.splitSpaces
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
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

        // If this message is a command we'll want to know about it later
        val isCommand = handleCommand(event)

        // Handle specific features for where the message was sent
        if (event.isFromGuild) handleGuildMessage(event, isCommand) else handlePrivateMessage(event)

    }

    @Suppress("unused")
    fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        val message = event.message
        // Once again ignore messages from bots or webhooks
        if (event.author.isBot || message.isWebhookMessage) return
        // TODO Recheck the message for invites and handle anti-advertise
    }

    private fun handleGuildMessage(event: MessageReceivedEvent, isCommand: Boolean) {
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

    private fun handleCommand(event: MessageReceivedEvent): Boolean {
        val content = event.message.contentRaw
        // Check if the message with a default or custom prefix
        val prefixUsed = getPrefixUsed(sandra, content, event.guild) ?: return false

        // Remove the prefix and isolate the first word
        val trimmedContent = content.substringAfter(prefixUsed).trim()
        val contentParts = trimmedContent.splitSpaces(2)
        val commandName = contentParts[0].lowercase()

        // Check if the first word is a command that exists
        val command = sandra.commands[commandName] ?: return false

        var args = if (contentParts.size == 1) "" else contentParts[1]
        // Check to see if a subcommand is being used
        val maybeSubcommand = if (args.isNotEmpty()) {
            command.findChild(args).also { args = it.second }.first ?: command
        } else command

        // Fire the command through our event system so the command is actually handled
        val commandEvent = CommandEvent(sandra, event, maybeSubcommand, args)
        sandra.eventManager.handleEvent(commandEvent)

        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MessageListener::class.java)
    }

}
