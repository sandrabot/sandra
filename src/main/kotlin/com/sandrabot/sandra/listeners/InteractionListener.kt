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
import com.sandrabot.sandra.utils.checkCommandBlocklist
import com.sandrabot.sandra.utils.missingPermission
import com.sandrabot.sandra.utils.missingSelfMessage
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.CoroutineEventListener
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory

/**
 * Event listener that processes events related to interactions.
 */
class InteractionListener(private val sandra: Sandra) : CoroutineEventListener {

    override suspend fun onEvent(event: GenericEvent) {
        when (event) {
            is SlashCommandInteractionEvent -> onSlashCommand(event)
        }
    }

    /**
     * Handler for all [SlashCommandInteractionEvent] events.
     */
    private suspend fun onSlashCommand(slashEvent: SlashCommandInteractionEvent) {
        // the command will only ever be null if it was manually removed with an eval
        val qualifiedPath = slashEvent.fullCommandName.replace(' ', '.')
        val command = sandra.commands[qualifiedPath] ?: run {
            logger.warn("Failed to find registered command with path: $qualifiedPath")
            return
        }
        // the first thing we want to do is wrap this with our own object
        val event = CommandEvent(sandra, slashEvent, command)
        // check the blocklist to prevent responding in actively blocked contexts
        if (checkCommandBlocklist(event)) return
        // do additional checks for guild commands, since discord added privileges we only need to check for ourselves
        if (slashEvent.isFromGuild) {
            // make sure we have all the permissions we'll need to run this command
            (requiredPermissions + command.requiredPermissions).find { missingPermission(event, it) }?.let {
                event.replyError(missingSelfMessage(event, it)).setEphemeral(true).await()
                return
            }
            // TODO user privileges and permissions
        } // we don't need to check guild only anymore, thanks discord
        if (command.ownerOnly && !event.isOwner) {
            event.replyError(event.getAny("core.owner_only"))
            return
        }
        // and now we can log the command and execute it
        val logUser = "${event.user.asTag} [${event.user.id}]"
        val logChannel = if (event.guild != null) {
            "${event.channel.name} [${event.channel.id}] | ${event.guild.name} [${event.guild.id}]"
        } else "direct message"
        logger.info("$qualifiedPath | $logUser | $logChannel | ${slashEvent.commandString}")
        // if the guild isn't fully loaded, that could affect some of our commands
        if (slashEvent.isFromGuild && event.guild?.isLoaded == false) event.guild.loadMembers().await()
        // catch any exceptions the commands could throw
        try {
            command.execute(event)
        } catch (e: MissingPermissionException) {
            event.sendError(missingSelfMessage(event, e.permission)).setEphemeral(true).queue()
            logger.debug("Couldn't finish command execution due to missing permissions", e)
        } catch (e: MissingArgumentException) {
            event.sendError(event.getAny("core.missing_argument", e.argument.name)).setEphemeral(true).queue()
        } catch (t: Throwable) {
            event.sendError(event.getAny("core.interaction_error")).setEphemeral(true).queue()
            logger.error("An exception occurred while executing a command", t)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(InteractionListener::class.java)
        private val requiredPermissions = arrayOf(Permission.MESSAGE_EXT_EMOJI)
    }

}
