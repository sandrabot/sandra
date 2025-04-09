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

package com.sandrabot.sandra.commands.essential

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.constants.asEmoji
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.messages.Embed
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.IOException
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class Feedback : Command() {
    override suspend fun execute(event: CommandEvent) {

        val selectMenu = StringSelectMenu("feedback:select:" + event.interaction.id, event.get("select_placeholder")) {
            option(event.get("bug_label"), "bug", event.get("bug_description"), Emoji.fromUnicode(Unicode.BUG))
            option(event.get("suggest_label"), "suggest", event.get("suggest_description"), Emotes.PROMPT.asEmoji())
            option(event.get("other_label"), "other", event.get("other_description"), Emotes.CHAT.asEmoji())
            option(event.get("cancel_label"), "cancel", event.get("cancel_description"), Emotes.LEAVE.asEmoji())
        }
        val hook = event.reply(event.get("disclaimer", Emotes.NOTICE)).addActionRow(selectMenu).asEphemeral().await()

        // allow the user some time to read the disclaimer, but don't wait too long to timeout
        val selectEvent = withTimeoutOrNull(2.minutes) {
            event.jda.await<StringSelectInteractionEvent> { it.componentId == selectMenu.id && it.user == event.user }
        }

        // just delete the message if it times out or the user cancels
        if (selectEvent == null || "cancel" in selectEvent.values) {
            // we don't need to acknowledge the select event if we're deleting the message anyway
            hook.deleteOriginal().queue(null, ERROR_HANDLER)
            return
        }

        val modal = Modal("feedback:modal:" + event.interaction.id, event.get("modal.title")) {
            short("summary", event.get("modal.summary_label"), required = true) {
                placeholder = event.get("modal.summary_placeholder")
                setRequiredRange(8, 50)
            }
            paragraph("description", event.get("modal.description_label"), required = true) {
                placeholder = event.get("modal.description_placeholder")
                setRequiredRange(20, 2000)
            }
        }

        // edit the message to say we're waiting for their response, thus removing inactive components
        hook.editOriginal(event.get("waiting", Emotes.LOADING)).setComponents().queue()
        selectEvent.replyModal(modal).queue()

        // let the user take their time to write out their thoughts, but don't
        // wait too long since interaction tokens expire after 15 minutes
        val modalEvent = withTimeoutOrNull(12.minutes) {
            event.jda.await<ModalInteractionEvent> { it.modalId == modal.id && it.user == event.user }
        } ?: run {
            hook.deleteOriginal().queue(null, ERROR_HANDLER)
            return
        }

        val type = selectEvent.values.first()
        val summary = modalEvent.getValue("summary")!!.asString.trim()
        val content = modalEvent.getValue("description")!!.asString.trim()
        val feedback = """
                |user. . . . . ${event.user.name} [${event.user.id}]
                |guild . . . . ${event.guild?.name} [${event.guild?.id}]
                |channel . . . ${event.channel.name} [${event.channel.id}]
                |interaction . ${event.interaction.name} [${event.interaction.id}]
                |type. . . . . $type
                |summary . . . $summary
                |description . $content
            """.trimMargin()

        // write all submissions to disk for record keeping
        File("feedback/").apply { mkdir() }.resolve("submission_${event.interaction.id}.txt").apply {
            try {
                writeText(feedback, Charsets.UTF_8)
            } catch (e: IOException) {
                LOGGER.error("Failed to write feedback report to $absolutePath\n$feedback", e)
            }
        }

        // after we've saved the file, we can consider this a success
        modalEvent.editMessage(event.get("thanks", Emotes.SUCCESS)).queue()

        // optionally post the feedback publicly in a channel
        val channel = event.sandra.shards.getTextChannelById(event.sandra.settings.features.feedbackChannel)
        if (channel == null || !channel.canTalk()) {
            // warn if for some reason the feedback channel is inaccessible
            LOGGER.warn("The feedback submission channel is currently unavailable! Currently selected channel: ${event.sandra.settings.features.feedbackChannel}")
            return
        }

        Embed {
            description = content
            val emoji = selectMenu.options.first { it.value == type }.emoji!!.let {
                if (it is CustomEmoji) it.asCustom().asMention else it.name
            }
            title = "$emoji ${event.get(type + "_label")} ${Unicode.BULLET} $summary"
            footer(event.get("footer", event.user.name, event.user.id), event.user.effectiveAvatarUrl)
        }.also { embed -> channel.sendMessageEmbeds(embed).queue() }
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(Feedback::class.java)
        val ERROR_HANDLER = ErrorHandler().ignore(
            ErrorResponse.UNKNOWN_INTERACTION, ErrorResponse.UNKNOWN_MESSAGE
        )
    }
}
