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

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.Category
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.exceptions.MissingArgumentException
import com.sandrabot.sandra.exceptions.MissingPermissionException
import com.sandrabot.sandra.utils.*
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.Permission
import org.slf4j.LoggerFactory

class CommandListener {

    @Suppress("unused")
    fun onCommand(event: CommandEvent) {

        val command = event.command
        val isFromGuild = event.isFromGuild

        // Check the blocklist to prevent processing in active contexts
        if (checkCommandBlocklist(event)) return

        // Do additional checks for guilds, mostly permission checks
        if (isFromGuild) {
            when {
                // Check for basic permissions we might need to reply
                missingPermission(event, Permission.MESSAGE_SEND) -> {
                    if (hasPermission(event, Permission.MESSAGE_ADD_REACTION)) {
                        event.message.addReaction(Unicode.SPEAK_NO_EVIL).queue()
                    } else logger.info("Cannot execute command: Missing MESSAGE_WRITE")
                    return
                }
                missingPermission(event, Permission.MESSAGE_EXT_EMOJI) -> {
                    val selfMessage = missingSelfMessage(event, Permission.MESSAGE_EXT_EMOJI)
                    event.channel.sendMessage(Unicode.CROSS_MARK + Unicode.VERTICAL_LINE + selfMessage).queue()
                    return
                }
                missingPermission(event, Permission.MESSAGE_HISTORY) -> {
                    val selfMessage = missingSelfMessage(event, Permission.MESSAGE_HISTORY)
                    event.channel.sendMessage(Emotes.ERROR + Unicode.VERTICAL_LINE + selfMessage).queue()
                    return
                }
            }
            // Ensure the command permission requirements are met
            for (permission in command.requiredPermissions) {
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
            event.replyError(event.translate("general.guild_only", false))
            return
        }

        if (command.category == Category.OWNER && !event.isOwner) {
            event.replyError(event.translate("general.owner_only", false))
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
            // Ensure that all members are loaded if this command is only for guilds
            if (command.guildOnly && !event.guild.isLoaded) event.guild.loadMembers().await()
            try {
                command.execute(event)
            } catch (e: MissingPermissionException) {
                event.replyError(missingSelfMessage(event, e.permission))
                logger.info("Cannot finish executing command due to missing permissions", e)
            } catch (e: MissingArgumentException) {
                event.replyError(event.translate("general.missing_argument", false, e.argument.name))
            } catch (e: Throwable) {
                event.replyError(event.translate("general.command_exception", false))
                logger.error("An exception occurred while executing a command", e)
            }
        }

    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommandListener::class.java)
    }

}
