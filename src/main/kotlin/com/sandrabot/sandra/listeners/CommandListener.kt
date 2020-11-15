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
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.entities.blocklist.FeatureType
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.exceptions.MissingArgumentException
import com.sandrabot.sandra.exceptions.MissingPermissionException
import com.sandrabot.sandra.utils.*
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory

class CommandListener(private val sandra: Sandra) {

    @Suppress("unused")
    fun onMessageReceived(event: MessageReceivedEvent) {

        if (event.author.isBot) return

        val content = event.message.contentRaw
        // Check if the message starts with a prefix
        val prefixUsed = getPrefixUsed(content) ?: return

        val contentParts = content.substring(prefixUsed.length).trim().splitSpaces(2)
        val name = contentParts[0].toLowerCase()
        // Check if a command exists with this name
        val command = sandra.commands.getCommand(name) ?: return

        var args = if (contentParts.size == 1) "" else contentParts[1]
        // Check to see if maybe a subcommand is being used
        val maybeSubcommand = if (args.isNotEmpty()) {
            var currentCommand: Command = command
            // Only keep looking while there's children to search
            while (currentCommand.children.isNotEmpty()) {
                val firstArg = args.splitSpaces()[0]
                // Find the first child with this name or alias
                val child = currentCommand.children.firstOrNull {
                    arrayOf(it.name, *it.aliases).any { alias ->
                        firstArg.equals(alias, ignoreCase = true)
                    }
                }
                // If a child was found remove the name from the args
                // And also keep checking if nested subcommands are being used
                if (child != null) {
                    args = args.replaceFirst(firstArg, "").removeExtraSpaces()
                    currentCommand = child
                } else break
            }
            currentCommand
        } else command

        val commandEvent = CommandEvent(sandra, event, maybeSubcommand, args)
        // Fire the event using our event system even though we handle them immediately below
        // This allows multiple core systems to listen for commands and react accordingly
        sandra.eventManager.handleEvent(commandEvent)

    }

    @Suppress("unused")
    fun onCommand(event: CommandEvent) {

        val command = event.command
        val isFromGuild = event.isFromGuild

        // Check the blocklist to prevent processing in active contexts
        if (checkBlocklist(event, FeatureType.COMMANDS)) return

        // Owners are exempt from cooldowns
        if (event.command.cooldown > 0 && !event.isOwner) {
            // Apply the cooldown and return if active
            if (event.sandra.cooldowns.applyCooldown(event)) return
        }

        // Do additional checks for guilds, mostly permission checks
        if (isFromGuild) {
            when {
                // Check for basic permissions we might need to reply
                missingPermission(event, Permission.MESSAGE_WRITE) -> {
                    if (hasPermission(event, Permission.MESSAGE_ADD_REACTION)) {
                        event.message.addReaction(Unicode.SPEAK_NO_EVIL).queue()
                    } else logger.info("Cannot execute command: Missing MESSAGE_WRITE")
                    return
                }
                missingPermission(event, Permission.MESSAGE_EXT_EMOJI) -> {
                    val selfMessage = missingSelfMessage(event, Permission.MESSAGE_EXT_EMOJI)
                    event.reply("${Unicode.CROSS_MARK} $selfMessage")
                    return
                }
            }
            // Ensure the command permission requirements are met
            for (permission in command.botPermissions) {
                if (missingPermission(event, permission)) {
                    event.replyError(missingSelfMessage(event, permission))
                    return
                }
            }
            // Owners are exempt from user permission checks
            if (!event.isOwner) for (permission in command.userPermissions) {
                if (missingUserPermission(event, permission)) {
                    event.replyError(missingUserMessage(event, permission))
                    return
                }
            }
        } else if (command.guildOnly) {
            event.replyError(event.translate("general.guild_only"))
            return
        }

        if (command.ownerOnly && !event.isOwner) {
            event.replyError(event.translate("general.owner_only"))
            return
        }

        // Only log the command when it is actually executed
        val author = "${event.author.asTag} [${event.author.id}]"
        val channel = if (event.isFromGuild) {
            "${event.textChannel.name} [${event.textChannel.id}] | ${event.guild.name} [${event.guild.id}]"
        } else "direct message"
        logger.info("${event.commandPath} | $author | $channel | ${event.message.contentRaw}")

        // Execute the command in a blocking coroutine, execute is a suspended function
        runBlocking {
            try {
                command.execute(event)
            } catch (e: MissingPermissionException) {
                event.replyError(missingSelfMessage(event, e.permission))
                logger.info("Cannot finish executing command due to missing permissions", e)
            } catch (e: MissingArgumentException) {
                event.replyError(event.translate("general.missing_argument", e.argument.name))
            } catch (e: Exception) {
                event.replyError(event.translate("general.command_exception"))
                logger.error("An exception occurred while executing a command", e)
            }
        }

    }

    private fun getPrefixUsed(content: String): String? {
        return sandra.commands.prefixes.find { content.startsWith(it, ignoreCase = true) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommandListener::class.java)
    }

}
