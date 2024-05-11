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

import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.asEmoji
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.option
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.FileUpload
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
        val interactionHook = event.reply(event.get("disclaimer", Emotes.NOTICE))
            .addActionRow(selectMenu).setEphemeral(true).await()

        // allow the user some time to read the disclaimer, but don't wait too long to timeout
        val selectEvent = withTimeoutOrNull(2.minutes) {
            event.jda.await<StringSelectInteractionEvent> { it.componentId == selectMenu.id && it.user == event.user }
        }

        // just delete the message if it times out or the user cancels
        if (selectEvent == null || "cancel" in selectEvent.values) {
            // we don't need to acknowledge the select event if we're deleting the message anyway
            interactionHook.deleteOriginal().queue(null, ERROR_HANDLER)
            return
        }

        val feedbackModal = Modal("feedback:modal:" + event.interaction.id, event.get("modal.title")) {
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
        interactionHook.editOriginal(event.get("waiting", Emotes.LOADING)).setComponents().queue()
        selectEvent.replyModal(feedbackModal).queue()

        // let the user take their time to write out their thoughts, but don't
        // wait too long since interaction tokens expire after 15 minutes
        val modalEvent = withTimeoutOrNull(12.minutes) {
            event.jda.await<ModalInteractionEvent> { it.modalId == feedbackModal.id && it.user == event.user }
        } ?: run {
            interactionHook.deleteOriginal().queue(null, ERROR_HANDLER)
            return
        }

        val feedbackType = selectEvent.values.first()
        val summary = modalEvent.getValue("summary")!!.asString.trimEnd()
        val description = modalEvent.getValue("description")!!.asString.trimEnd()

        val feedbackChannel = event.sandra.shards.getTextChannelById(event.sandra.settings.features.feedbackChannel)
        // if for some reason the feedback channel is inaccessible, allow the user to download their input
        if (feedbackChannel == null || !feedbackChannel.canTalk()) {
            val content = """
                |feedback type: $feedbackType
                |summary: $summary
                |
                |$description
            """.trimMargin()
            modalEvent.editMessage(event.get("unavailable", Constants.DIRECT_SUPPORT))
                .setFiles(FileUpload.fromData(content.toByteArray(), "feedback.txt")).queue()
            return
        }

        val title = event.get(feedbackType + "_label")
        val emoji = selectMenu.options.first { it.value == feedbackType }.emoji!!.let {
            if (it.type == Emoji.Type.CUSTOM) it.asCustom().asMention else it.name
        }
        val embed = event.embed.setTitle("$emoji $title ${Unicode.BULLET} $summary").setDescription(description)
            .setFooter(event.get("footer", event.user.name, event.user.id), event.user.effectiveAvatarUrl)

        feedbackChannel.sendMessageEmbeds(embed.build()).await()
        modalEvent.editMessage(event.get("thanks", Emotes.SUCCESS)).queue()

    }

    private companion object {
        val ERROR_HANDLER = ErrorHandler().ignore(ErrorResponse.UNKNOWN_INTEGRATION)
    }
}
