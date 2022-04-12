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
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.exceptions.MissingArgumentException
import com.sandrabot.sandra.exceptions.MissingPermissionException
import com.sandrabot.sandra.utils.await
import com.sandrabot.sandra.utils.checkCommandBlocklist
import com.sandrabot.sandra.utils.missingPermission
import com.sandrabot.sandra.utils.missingSelfMessage
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory

class CommandListener(val sandra: Sandra) {

    @Suppress("unused")
    fun onSlashCommand(slashEvent: SlashCommandInteractionEvent) {

        // The command could only be null if it was removed with an eval
        val command = sandra.commands[slashEvent.commandPath] ?: run {
            logger.warn("Could not find command path for registered command: ${slashEvent.commandPath}")
            return
        }
        val event = CommandEvent(sandra, slashEvent, command)
        val isFromGuild = slashEvent.isFromGuild

        // Check the blocklist to prevent responding to active contexts
        if (checkCommandBlocklist(event)) return

        // Do additional checks for guilds, mostly just permission checks
        if (isFromGuild) {
            // Make sure we have all the permissions we need to run correctly
            requiredPermissions.firstOrNull { missingPermission(event, it) }?.let {
                event.replyError(missingSelfMessage(event, it)).setEphemeral(true).queue()
                return
            }
            // Ensure the required permissions are met for this command
            for (permission in command.requiredPermissions) {
                if (missingPermission(event, permission)) {
                    event.replyError(missingSelfMessage(event, permission)).setEphemeral(true).queue()
                    return
                }
            }
            // TODO User privilege and permissions
        } else if (command.guildOnly) {
            event.replyError(event.getAny("general.guild_only")).setEphemeral(true).queue()
            return
        }

        if (command.ownerOnly && !event.isOwner) {
            event.replyError(event.getAny("general.owner_only")).setEphemeral(true).queue()
            return
        }

        // Finally, log the command when it is actually executed
        val user = "${event.user.asTag} [${event.user.id}]"
        val channel = if (event.guild != null) {
            "${event.channel.name} [${event.channel.id}] | ${event.guild.name} [${event.guild.id}]"
        } else "direct message"
        logger.info("${event.commandPath} | $user | $channel | ${slashEvent.commandString}")

        // Execute the command in a blocking coroutine, execute is a suspended function
        // We are technically already within a coroutine from the event being fired
        runBlocking {
            // Ensure the guild is loaded if ran from a guild
            if (command.guildOnly && !event.guild!!.isLoaded) event.guild.loadMembers().await()
            try {
                command.execute(event)
            } catch (e: MissingPermissionException) {
                event.replyError(missingSelfMessage(event, e.permission)).setEphemeral(true).queue()
                logger.info("Cannot finish executing command due to missing permissions", e)
            } catch (e: MissingArgumentException) {
                event.replyError(event.getAny("general.missing_argument", e.argument.name))
                    .setEphemeral(true).queue()
            } catch (t: Throwable) {
                event.sendError(event.getAny("general.command_exception")).setEphemeral(true).queue()
                logger.error("An exception occurred while executing a command", t)
            }
        }

    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommandListener::class.java)
        private val requiredPermissions = arrayOf(Permission.MESSAGE_EXT_EMOJI)
    }

}
