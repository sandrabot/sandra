/*
 * Copyright 2017-2020 Avery Clifton and Logan Devecka
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
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.blocklist.FeatureType
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.exceptions.MissingPermissionException
import com.sandrabot.sandra.utils.hasPermission
import com.sandrabot.sandra.utils.missingPermission
import com.sandrabot.sandra.utils.missingSelfMessage
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory

class CommandListener(private val sandra: Sandra) {

    @Suppress("unused")
    fun onMessageReceived(event: MessageReceivedEvent) {

        if (event.author.isBot) return

        // TODO Blocklist notify, utility method?
        if (sandra.blocklist.isFeatureBlocked(event.author.idLong, FeatureType.COMMANDS)) return
        if (event.isFromGuild && sandra.blocklist.isFeatureBlocked(event.guild.idLong, FeatureType.COMMANDS)) return

        val content = event.message.contentRaw
        // Check if the message starts with a prefix
        val prefixUsed = getPrefixUsed(content) ?: return

        val contentParts = content.substring(prefixUsed.length).trim().split(spaceRegex, 2)
        val name = contentParts[0].toLowerCase()
        // Check if a command exists with this name
        val command = sandra.commands.getCommand(name) ?: return

        val author = "${event.author.asTag} [${event.author.id}]"
        val channel = if (event.isFromGuild) {
            "${event.textChannel.name} [${event.textChannel.id}] | ${event.guild.name} [${event.guild.id}]"
        } else "direct message"
        logger.info("$name | $author | $channel | $content")

        val args = if (contentParts.size == 1) "" else contentParts[1]
        val commandEvent = CommandEvent(sandra, event, command, args)
        // Fire the event using our event system even though we handle them immediately below
        // This allows multiple core systems to listen for commands and react accordingly
        sandra.eventManager.handleEvent(commandEvent)

    }

    @Suppress("unused")
    fun onCommand(event: CommandEvent) {

        val command = event.command

        // TODO Handle subcommands

        // TODO Check cooldown to prevent reacting to spam

        // Check for basic permissions when the command is used in a server
        if (event.isFromGuild) when {
            missingPermission(event, Permission.MESSAGE_WRITE) -> {
                if (hasPermission(event, Permission.MESSAGE_ADD_REACTION)) {
                    event.message.addReaction(Unicode.SPEAK_NO_EVIL).queue()
                } else logger.info("Cannot execute command: Couldn't indicate missing MESSAGE_WRITE")
                return
            }
            missingPermission(event, Permission.MESSAGE_EXT_EMOJI) -> {
                event.reply("${Unicode.CROSS_MARK} ${missingSelfMessage(event, Permission.MESSAGE_EXT_EMOJI)}")
                return
            }
        } else if (command.guildOnly) {
            event.replyError(event.translate("general.guild_only"))
            return
        }

        if (command.ownerOnly && !event.isOwner) {
            event.replyError(event.translate("general.owner_only"))
            return
        }

        // Execute the command in a blocking coroutine, execute is a suspended function
        runBlocking {
            try {
                command.execute(event)
            } catch (e: MissingPermissionException) {
                event.replyError(missingSelfMessage(event, e.permission))
                logger.info("Cannot finish executing command due to missing permissions", e)
            } catch (e: Exception) {
                event.replyError(event.translate("general.command_exception"))
                logger.error("An exception occurred while executing a command", e)
            }
        }

    }

    private fun getPrefixUsed(content: String): String? {
        return sandra.commands.prefixes.firstOrNull { content.startsWith(it, ignoreCase = true) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommandListener::class.java)
        private val spaceRegex = Regex("""\s+""")
    }

}
