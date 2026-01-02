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

package com.sandrabot.sandra.commands.moderation

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.utils.sanitize
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.MessageEdit
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class SoftBan : Command(
    arguments = "[@user] [reason:text] [quiet:boolean]",
    guildOnly = true,
    selfPermissions = setOf(Permission.BAN_MEMBERS),
    userPermissions = setOf(Permission.BAN_MEMBERS)
) {

    override suspend fun execute(event: CommandEvent) {

        // will never be null, user is a required argument
        val targetUser = event.arguments.user()!!
        // prevent moderators from accidentally banning themselves
        if (targetUser == event.user) {
            event.replyError(event.get("no_self")).asEphemeral().queue()
            return
        }
        val isQuiet = event.arguments.boolean("quiet") ?: false
        event.deferReply(ephemeral = isQuiet).queue()
        // check the banlist to see if the target user is already banned
        event.guild!!.retrieveBan(targetUser).onErrorMap { null }.await()?.let { userBan ->
            val realReason = userBan.reason?.sanitize() ?: event.get("default_reason")
            event.replyEmoji(Emotes.BAN, event.get("already_banned", targetUser.name, realReason)).queue()
            return
        }
        // verify permission hierarchy for all parties involved
        event.guild.retrieveMember(targetUser).onErrorMap { null }.await()?.let { targetMember ->
            val errorMessage = when {
                targetMember.isOwner -> "no_owner"
                !event.member!!.canInteract(targetMember) -> "no_interact"
                !event.selfMember!!.canInteract(targetMember) -> "no_interact_self"
                else -> return@let
            }
            event.replyError(event.get(errorMessage, targetMember.asMention)).asEphemeral().queue()
            return
        }

        // allow the moderator to double-check the user they've selected
        event.sendMessage(MessageCreate(useComponentsV2 = true) {
            container {
                text(event.get("confirmation", Emotes.MOD, targetUser.asMention))
                actionRow {
                    dangerButton("confirm:${event.id}", event.get("button_confirm"))
                    secondaryButton("cancel:${event.id}", event.get("button_cancel"))
                }
            }
        }).setAllowedMentions(emptySet()).queue()

        // allow 1 minute for the moderator to make a selection
        val buttonEvent = withTimeoutOrNull(1.minutes) {
            while (true) {
                // this is a blocking call while we wait for the button click
                val interaction = event.jda.await<ButtonInteractionEvent> { "${event.id}" in it.componentId }
                // verify who actually clicked the button, since anyone could've done it
                if (interaction.user == event.user) return@withTimeoutOrNull interaction
                // otherwise just acknowledge the click and wait for another one
                interaction.deferEdit().queue()
            }
        } as ButtonInteractionEvent?

        if (buttonEvent == null || "cancel" in buttonEvent.componentId) {
            event.hook.deleteOriginal().queue(null, ERROR_HANDLER)
            return
        }

        val reason = event.arguments.text("reason") ?: event.get("default_reason")
        val realReason = event.get("reason", event.user.name, reason)

        var banNotification: Message? = null
        if (!targetUser.isBot && !targetUser.isSystem) {
            // attempt to send the user a ban notification with the reason provided
            val message = event.get("ban_notification", Emotes.NOTICE, event.guild.name.sanitize(), event.user.name, reason)
            banNotification = targetUser.openPrivateChannel().flatMap { it.sendMessage(message) }.onErrorMap { null }.await()
        }

        // automatically delete any messages the target user sent within the past day
        event.guild.ban(targetUser, 1, TimeUnit.DAYS).reason(realReason)
            .flatMap { event.guild.unban(targetUser).reason(realReason) }.flatMap {
                event.hook.editOriginal(MessageEdit(useComponentsV2 = true) {
                    container { text(event.get("success", Emotes.SUCCESS, targetUser.name)) }
                })
            }.onErrorFlatMap {
                banNotification?.delete()?.queue()
                event.hook.editOriginal(MessageEdit(useComponentsV2 = true) {
                    container { text(Emotes.FAILURE + " " + event.getAny("core.interaction_error")) }
                })
            }.queue()

    }

    private companion object {
        private val ERROR_HANDLER = ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
    }

}
