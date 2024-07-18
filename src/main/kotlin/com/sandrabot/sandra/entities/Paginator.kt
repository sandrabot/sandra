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

package com.sandrabot.sandra.entities

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.truncate
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.interactions.components.ModalBuilder
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditData
import kotlin.time.Duration.Companion.minutes

/**
 * State machine that facilitates the pagination of [MessageEmbed] objects as command responses.
 *
 * * When [showPageNumbers] is `true`, the footer of the embeds will be prepended with a page indicator.
 * * When [showPageJumper] is `true`, an additional "jump to page" button will be available to the user.
 * * When [textProvider] is non-null, it will be called each time the page is updated, allowing text to be updated.
 */
class Paginator(
    private val commandEvent: CommandEvent,
    private val showPageNumbers: Boolean = true,
    private val showPageJumper: Boolean = true,
    private val textProvider: ((Int) -> String?)? = null
) {

    // create unique instances of component ids for this interaction
    private val jumpModalId = "page:modal:" + commandEvent.interaction.id
    private val jumpInputId = "page:input:" + commandEvent.interaction.id
    private val backButtonId = "page:back:" + commandEvent.interaction.id
    private val jumpButtonId = "page:jump:" + commandEvent.interaction.id
    private val nextButtonId = "page:next:" + commandEvent.interaction.id
    private val exitButtonId = "page:exit:" + commandEvent.interaction.id

    private val buttons = listOf(
        Button.primary(backButtonId, Emoji.fromFormatted(Emotes.ARROW_LEFT)),
        Button.secondary(jumpButtonId, Emoji.fromFormatted(Emotes.NUMBER)),
        Button.primary(nextButtonId, Emoji.fromFormatted(Emotes.ARROW_RIGHT)),
        Button.danger(exitButtonId, Emoji.fromFormatted(Emotes.RETURN))
    ).map { button ->
        // dynamically localize the button labels for the end user
        val name = button.id!!.substringAfter(':').substringBefore(':')
        button.withLabel(commandEvent.getAny("core.paginator.buttons.$name"))
    }.toMutableList()
    private val messageEmbeds = mutableListOf<MessageEmbed>()

    private var currentPage: Int = 0
    private var messageId: Long = 0
        set(value) = if (field != 0L) throw IllegalStateException("MessageId is already set") else field = value

    /**
     * Initializes the paginator using the [pages] and options provided.
     * @see initialize
     */
    suspend fun paginate(pages: List<MessageEmbed>, startingPage: Int = 0) = initialize(pages, startingPage)

    private suspend fun initialize(pages: List<MessageEmbed>, startingPage: Int) {
        // check to make sure we (hopefully) don't have any issues initializing the paginator
        if (pages.isEmpty()) throw IllegalArgumentException("Pages must not be empty")
        if (messageEmbeds.isNotEmpty()) throw IllegalStateException("Paginator has already been initialized")
        if (currentPage !in pages.indices) throw IllegalStateException("Starting page $currentPage must be within ${pages.indices}")
        currentPage = startingPage

        // render the page indicators if enabled and applicable
        messageEmbeds += if (showPageNumbers && pages.size > 1) pages.mapIndexed { index, page ->
            val pageIndicator = commandEvent.getAny("paginator.page_indicator", index + 1, pages.size)
            val newFooter = page.footer?.text?.let { "$pageIndicator ${Unicode.BULLET} $it" }
            // rebuild the embed while retaining the old content and footer icon
            EmbedBuilder(page).setFooter(newFooter, page.footer?.iconUrl).build()
        } else pages // otherwise we don't need to do anything extra

        // remove the jump button from the list if it's not applicable
        if (pages.size < 2 || !showPageJumper) buttons.removeAll { it.id == jumpButtonId }
        // update the button states to reflect the current page
        updateButtonStates()

        // generate the message data and send the initial message
        messageId = commandEvent.sendMessage(generateMessageData()).await().idLong

        while (true) withTimeoutOrNull(1.minutes) {
            // await is a blocking call, this is where we actually wait for the event
            when (val event = commandEvent.sandra.shards.await<GenericInteractionCreateEvent>()) {
                // verify and handle the events if they're valid, then do it all over again
                is ButtonInteractionEvent -> if (verifyButton(event)) handleButton(event)
                is ModalInteractionEvent -> if (verifyModal(event)) handleModal(event)
            } // when the event listener times out, cancel the menu
        } ?: break

        // destroy the paginator when the menu is cancelled
        commandEvent.hook.editMessageComponentsById(messageId).queue(null, handler)
    }

    private fun verifyButton(buttonEvent: ButtonInteractionEvent): Boolean {
        // only acknowledge our own button clicks
        if (buttonEvent.componentId !in buttons.map { it.id }) return false
        // only let the command author advance the pages
        return if (buttonEvent.user.idLong == commandEvent.user.idLong) {
            // verify that the button is actually enabled
            !buttons.first { buttonEvent.componentId == it.id }.isDisabled
        } else {
            // acknowledge the interaction but ignore it
            buttonEvent.deferEdit().queue()
            false
        }
    }

    private suspend fun handleButton(buttonEvent: ButtonInteractionEvent) = when (buttonEvent.componentId) {
        // exit the paginator when the exit button is clicked
        exitButtonId -> null

        // respond by replying to the interaction with a modal
        jumpButtonId -> buttonEvent.replyModal(
            ModalBuilder(jumpModalId, commandEvent.getAny("paginator.modal_title")) {
                short(jumpInputId, commandEvent.getAny("paginator.input_label")) {
                    placeholder = commandEvent.getAny("paginator.input_placeholder", messageEmbeds.size)
                    maxLength = messageEmbeds.size.toString().length
                    isRequired = true
                }
            }.build()
        ).queue()

        else -> {
            // these are the only buttons that actually change the page
            when (buttonEvent.componentId) {
                // we can assume the page should always be updated
                backButtonId -> currentPage--
                nextButtonId -> currentPage++
            }
            updateMessage(buttonEvent)
        }
    }

    private fun verifyModal(modalEvent: ModalInteractionEvent) =
        modalEvent.message?.idLong == messageId && modalEvent.modalId == jumpModalId && modalEvent.user.idLong == commandEvent.user.idLong

    private suspend fun handleModal(modalEvent: ModalInteractionEvent) {
        val digit = modalEvent.getValue(jumpInputId)!!.asString
        // attempt to convert the string to an integer, if not stay on the same page
        val pageIndex = digit.toIntOrNull()?.let { it - 1 } ?: currentPage
        // if the index isn't within range, we'll just ignore it and update the message anyway
        if (pageIndex in messageEmbeds.indices) currentPage = pageIndex
        updateMessage(modalEvent)
    }

    private fun generateMessageData() = MessageCreateBuilder().setContent(
        textProvider?.invoke(currentPage)?.truncate(Message.MAX_CONTENT_LENGTH)
    ).setEmbeds(messageEmbeds[currentPage]).setActionRow(buttons).build()

    private suspend fun updateMessage(editCallback: IMessageEditCallback) {
        // update the button states before generating the edit data
        updateButtonStates()
        // if the interaction was already acknowledged, use the webhook to edit
        val editData = MessageEditData.fromCreateData(generateMessageData())
        if (editCallback.isAcknowledged) {
            editCallback.hook.editMessageById(messageId, editData).await()
        } else editCallback.editMessage(editData).await()
    }

    private fun updateButtonStates() = buttons.replaceAll { button ->
        when (button.id) {
            backButtonId -> button.withDisabled(currentPage == 0)
            nextButtonId -> button.withDisabled(currentPage == messageEmbeds.lastIndex)
            else -> button
        }
    }

    private companion object {
        private val handler = ErrorHandler().ignore(ErrorResponse.UNKNOWN_INTERACTION, ErrorResponse.UNKNOWN_MESSAGE)
    }

}
