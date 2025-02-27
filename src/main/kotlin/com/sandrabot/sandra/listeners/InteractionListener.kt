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
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.exceptions.MissingArgumentException
import com.sandrabot.sandra.exceptions.MissingPermissionException
import com.sandrabot.sandra.utils.checkCommandBlocklist
import com.sandrabot.sandra.utils.missingPermissionMessage
import dev.minn.jda.ktx.events.CoroutineEventListener
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.ContextException
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
        // the command will only ever be null if it failed to load, or it was disabled manually
        val path = slashEvent.fullCommandName.replace(' ', '.')
        val command = sandra.commands[path] ?: run {
            LOGGER.warn("Received an unknown command that should be loaded: $path", ContextException())
            return
        }

        // the first thing we want to do is wrap this with our own object
        val event = CommandEvent(command, slashEvent, sandra)
        // check the blocklist to prevent responding in actively blocked contexts
        if (checkCommandBlocklist(event)) return
        // restrict owner commands from being used by anyone
        if (command.isOwnerOnly && !event.isOwner) {
            event.replyError(event.getAny("core.owner_only")).asEphemeral().queue()
            return
        }

        // do additional checks for commands sent from guilds
        if (slashEvent.isFromGuild) {
            // discord should take care of these permissions for us, but we'll double-check them anyway
            command.selfPermissions.find { !event.selfMember!!.hasPermission(slashEvent.guildChannel, it) }?.let {
                event.replyError(event.missingPermissionMessage(it)).asEphemeral().queue()
                return
            }
            command.userPermissions.find { !event.member!!.hasPermission(slashEvent.guildChannel, it) }?.let {
                event.replyError(event.missingPermissionMessage(it, self = false)).asEphemeral().queue()
                return
            }
        }

        // log the command context information for usage history
        val channel = if (event.guild == null) "direct message"
        else "${event.channel.name} [${event.channel.id}] | ${event.guild.name} [${event.guild.id}]"
        LOGGER.info("$path | ${event.user.name} [${event.user.id}] | $channel | ${slashEvent.commandString}")

        // execute the command, catch any exceptions and log them
        try {
            command.execute(event)
        } catch (t: Throwable) {
            LOGGER.error("An exception occurred while executing a command", t)
            // ensure that the user receives an error message explaining the issue
            val message = when (t) {
                is MissingPermissionException -> event.missingPermissionMessage(t.permission)
                is MissingArgumentException -> event.getAny("core.missing_argument", t.argument.name)
                else -> event.getAny("core.interaction_error")
            }
            if (event.isAcknowledged) event.sendError(message).queue()
            else event.replyError(message).asEphemeral().queue()
        }
    }

    private companion object {
        private val LOGGER = LoggerFactory.getLogger(InteractionListener::class.java)
    }

}
