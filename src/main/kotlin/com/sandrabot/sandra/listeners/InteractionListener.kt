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
import com.sandrabot.sandra.exceptions.MissingArgumentException
import com.sandrabot.sandra.exceptions.MissingPermissionException
import com.sandrabot.sandra.utils.checkCommandBlocklist
import com.sandrabot.sandra.utils.isMissingPermission
import com.sandrabot.sandra.utils.missingPermissionMessage
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
            logger.warn("Received a command that is not registered with path: $qualifiedPath")
            return
        }
        // the first thing we want to do is wrap this with our own object
        val event = CommandEvent(command, slashEvent, sandra)
        // check the blocklist to prevent responding in actively blocked contexts
        if (checkCommandBlocklist(event)) return
        // do additional checks for guild commands, since discord added privileges we only need to check for ourselves
        if (slashEvent.isFromGuild) {
            // make sure we have all the permissions we'll need to run this command
            val allPermissions = basePermissions + command.selfPermissions
            allPermissions.find { event.isMissingPermission(it) }?.let {
                event.replyError(event.missingPermissionMessage(it, self = true)).setEphemeral(true).queue()
                return
            }
        }
        if (command.isOwnerOnly && !event.isOwner) {
            event.replyError(event.getAny("core.owner_only")).setEphemeral(true).queue()
            return
        }
        // and now we can log the command and execute it
        val logUser = "${event.user.name} [${event.user.id}]"
        val logChannel = if (event.guild != null) {
            "${event.channel.name} [${event.channel.id}] | ${event.guild.name} [${event.guild.id}]"
        } else "direct message"
        logger.info("$qualifiedPath | $logUser | $logChannel | ${slashEvent.commandString}")
        // catch any exceptions the commands could throw
        try {
            command.execute(event)
        } catch (t: Throwable) {
            logger.error("An exception occurred while executing a command", t)
            val message = when (t) {
                is MissingPermissionException -> event.missingPermissionMessage(t.permission, self = true)
                is MissingArgumentException -> event.getAny("core.missing_argument", t.argument.name)
                else -> event.getAny("core.interaction_error")
            }
            if (event.isAcknowledged) event.sendError(message).queue()
            else event.replyError(message).setEphemeral(true).queue()
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(InteractionListener::class.java)
        private val basePermissions = setOf(Permission.MESSAGE_EXT_EMOJI)
    }

}
