/*
 * Copyright 2026 Avery Carroll, Logan Devecka, and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandrabot.sandra.commands.essential

import com.sandrabot.sandra.constants.Emojis
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import dev.minn.jda.ktx.emoji.toEmoji
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.TextInput
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class Feedback : Command() {

    override suspend fun execute(event: CommandEvent) {

        // build the selection menu for the type of feedback the user intends to submit
        val selectMenu = StringSelectMenu("select:${event.id}", placeholder = event.get("placeholder")) {
            option(event.get("bug_label"), "bug", event.get("bug_description"), Unicode.BUG.toEmoji())
            option(event.get("suggest_label"), "suggest", event.get("suggest_description"), Emojis.PROMPT.toEmoji())
            option(event.get("other_label"), "other", event.get("other_description"), Emojis.CHAT.toEmoji())
            option(event.get("cancel_label"), "cancel", event.get("cancel_description"), Emojis.LEAVE.toEmoji())
        }

        // send the confirmation message along with the selection menu
        event.reply(MessageCreate(useComponentsV2 = true) {
            container {
                text(event.get("disclaimer", Emojis.NOTICE))
                actionRow(selectMenu)
            }
        }).asEphemeral().queue()

        // wait for the user to read the disclaimer before making a selection
        val selectEvent = withTimeoutOrNull(2.minutes) {
            // blocking call to await the next interaction event that matches the select menu
            event.jda.await<StringSelectInteractionEvent> { it.componentId == selectMenu.customId }
        }

        // delete the original message after making a selection or timing out
        event.hook.deleteOriginal().queue()
        // return early if the interaction timed out or the user selected cancel
        if (selectEvent == null || "cancel" in selectEvent.values) return

        val context = event.localeContext.withRoot("modal")
        // create the modal menu to collect the user's feedback
        val modal = Modal("modal:${event.id}", context["title"]) {
            val summary = TextInput("summary", TextInputStyle.SHORT, required = true) {
                placeholder = context["summary_placeholder"]
                requiredLength = 8..50
            }
            val description = TextInput("description", TextInputStyle.PARAGRAPH, required = true) {
                placeholder = context["description_placeholder"]
                requiredLength = 20..2000
            }
            label(context["summary_label"], child = summary)
            label(context["description_label"], child = description)
        }

        // reply to the selection event with the modal
        selectEvent.replyModal(modal).queue()

        // allow some time for the user to compose their thoughts
        // don't wait for too long since interaction tokens expire after 15 minutes
        val modalEvent = withTimeoutOrNull(14.minutes) {
            // blocking call to await the next interaction event that matches the modal
            event.jda.await<ModalInteractionEvent> { it.modalId == modal.id }
        } ?: return

        // gather the feedback from the modal interaction event
        val type = selectEvent.values.first()
        val summary = modalEvent.getValue("summary")!!.asString.trim()
        val content = modalEvent.getValue("description")!!.asString.trim()

        // create some context for the feedback report
        val feedback = """
                |user. . . . . ${event.user.name} [${event.user.id}]
                |guild . . . . ${event.guild?.name} [${event.guild?.id}]
                |channel . . . ${event.channel.name} [${event.channel.id}]
                |interaction . ${event.interaction.name} [${event.interaction.id}]
                |type. . . . . $type
                |summary . . . $summary
                |description . $content
            """.trimMargin()

        try {
            // attempt to create a feedback directory and write the report to disk
            File("feedback/").apply { mkdir() }.resolve("submission_${event.interaction.id}.txt").apply {
                writeText(feedback, Charsets.UTF_8)
            }
        } catch (t: Throwable) {
            // if we can't create the file for whatever reason, log the feedback instead
            LOGGER.error("Failed to write feedback report, are we missing permissions?\n$feedback", t)
        }

        // after we've saved the feedback we can consider this a success
        // reply to the modal event with a thank-you message
        modalEvent.reply(MessageCreate(useComponentsV2 = true) {
            container {
                text(event.get("thanks", Emojis.SUCCESS))
            }
        }).asEphemeral().queue()

        // if enabled, send the feedback to a specific text channel if we can, otherwise return early
        val channelId = event.sandra.settings.features.feedbackChannel.takeIf { it != 0L } ?: return
        val feedbackChannel = event.sandra.shards.getTextChannelById(channelId)
        // make sure the text channel actually exists, and that we can send messages there
        if (feedbackChannel == null || !feedbackChannel.canTalk()) {
            LOGGER.warn("Unable to access the configured feedback channel: $feedbackChannel")
            return
        }

        // attempt to send an embed to the feedback channel
        feedbackChannel.sendMessageEmbeds(Embed {
            description = content
            // determine the emoji for the selected feedback type
            val emoji = selectMenu.options.first { it.value == type }.emoji!!.formatted
            title = "$emoji ${event.get(type + "_label")} ${Unicode.BULLET} $summary"
            footer(event.get("footer", event.user.name, event.user.id), event.user.effectiveAvatarUrl)
        }).queue(null) {
            LOGGER.warn("Failed to send a message in the configured feedback channel: $feedbackChannel")
        }
    }

    private companion object {
        private val LOGGER = LoggerFactory.getLogger(Feedback::class.java)
    }

}
