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

import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.truncate
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import kotlin.time.Duration.Companion.minutes

/**
 * Allows you to easily paginate command responses using [MessageEmbed] as pages in the current [context].
 * You may use [contentProvider] to set custom message content for each page when necessary.
 */
class Paginator(private val context: CommandEvent, private val contentProvider: ((Int) -> String?)? = null) {

    // create unique component ids for this interaction
    private val backButtonId = "paginator:back:${context.id}"
    private val pageButtonId = "paginator:page:${context.id}"
    private val nextButtonId = "paginator:next:${context.id}"
    private val jumpModalId = "paginator:modal:${context.id}"
    private val jumpInputId = "paginator:input:${context.id}"

    private val messageData = mutableListOf<MessageCreateData>()

    private var currentPage: Int = 0
    private var messageId: Long = 0
        set(value) = if (field != 0L) throw IllegalStateException("MessageId was already set") else field = value

    /**
     * Initializes the paginator using the [pages] provided.
     * The first page to be displayed can be customized with [startingPage].
     */
    suspend fun paginate(pages: List<MessageEmbed>, startingPage: Int = currentPage) = initialize(pages, startingPage)

    private suspend fun initialize(pages: List<MessageEmbed>, page: Int) {
        if (pages.isEmpty()) throw IllegalArgumentException("Pages must not be empty")
        if (currentPage !in pages.indices) throw IllegalArgumentException("Starting page must be in range ${pages.indices}")
        if (messageData.isNotEmpty()) throw IllegalStateException("Paginator is already initialized")

        currentPage = page
        generateMessageData(pages)

        messageId = context.sendMessage(messageData[currentPage]).await().idLong

        while (true) withTimeoutOrNull(5.minutes) {
            when (val event = context.jda.await<GenericInteractionCreateEvent>()) {
                is ButtonInteractionEvent -> if (verifyButton(event)) handleButton(event)
                is ModalInteractionEvent -> if (verifyModal(event)) handleModal(event)
            }
        } ?: break

        // disable the buttons when the menu times out
        val disabled = messageData[currentPage].components.flatMap { it.buttons }.map { it.asDisabled() }
        context.hook.editMessageComponentsById(messageId, ActionRow.of(disabled)).queue(null, ERROR_HANDLER)
    }

    private fun verifyButton(event: ButtonInteractionEvent): Boolean {
        val currentButtons = messageData[currentPage].components.flatMap { it.buttons }
        // only acknowledge button clicks that belong to this paginator
        if (event.button !in currentButtons) return false
        // only allow the command author to advance the pages
        return if (event.user == context.user) {
            // verify that the button is actually enabled
            !currentButtons.first { event.componentId == it.id }.isDisabled
        } else {
            // acknowledge the interaction but ignore it
            event.deferEdit().queue()
            false
        }
    }

    private fun handleButton(event: ButtonInteractionEvent) = when (event.componentId) {
        pageButtonId -> event.replyModal(Modal(jumpModalId, context.getAny("core.paginator.modal_title")) {
            short(jumpInputId, context.getAny("core.paginator.input_label")) {
                placeholder = context.getAny("core.paginator.input_placeholder", messageData.size)
                maxLength = messageData.size.toString().length
                isRequired = true
            }
        }).queue()

        nextButtonId -> {
            currentPage++
            updateMessage(event)
        }

        backButtonId -> {
            currentPage--
            updateMessage(event)
        }

        else -> throw AssertionError()
    }

    private fun verifyModal(event: ModalInteractionEvent): Boolean =
        event.modalId == jumpModalId && event.user == context.user

    private fun handleModal(event: ModalInteractionEvent) {
        val digit = event.getValue(jumpInputId)!!.asString
        // attempt to convert the string to an integer
        val index = digit.toIntOrNull()?.let { it - 1 }
        // verify the index is within range, otherwise just acknowledge and return
        if (index != null && index in messageData.indices) currentPage = index else {
            event.deferEdit().queue()
            return
        }
        updateMessage(event)
    }

    private fun updateMessage(callback: IMessageEditCallback) {
        // if the callback has already been acknowledged, use the hook instead
        val messageData = MessageEditData.fromCreateData(messageData[currentPage])
        if (callback.isAcknowledged) {
            callback.hook.editMessageById(messageId, messageData).queue(null, ERROR_HANDLER)
        } else callback.editMessage(messageData).queue(null, ERROR_HANDLER)
    }

    private fun generateMessageData(pages: List<MessageEmbed>) = pages.mapIndexed { index, embed ->
        MessageCreate {
            content = contentProvider?.invoke(index)?.truncate(Message.MAX_CONTENT_LENGTH)
            embeds += embed
            actionRow(
                Button.primary(
                    backButtonId, Emoji.fromUnicode(Unicode.LEFT_BACKHAND)
                ).withDisabled(index == 0),

                Button.secondary(
                    pageButtonId, context.getAny("core.paginator.page", index + 1, pages.size)
                ).withDisabled(pages.size < 2),

                Button.primary(
                    nextButtonId, Emoji.fromUnicode(Unicode.RIGHT_BACKHAND)
                ).withDisabled(index == pages.lastIndex),
            )
        }
    }.let { messageData += it }

    private companion object {
        private val ERROR_HANDLER = ErrorHandler().ignore(
            ErrorResponse.UNKNOWN_INTERACTION, ErrorResponse.UNKNOWN_MESSAGE
        )
    }

}
