/*
 * Copyright 2017-2021 Avery Carroll and Logan Devecka
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
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.argumentAction
import com.sandrabot.sandra.utils.ensurePermissions
import com.sandrabot.sandra.utils.hasPermissions
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import net.dv8tion.jda.api.requests.ErrorResponse
import java.util.concurrent.TimeUnit

/**
 * Facilitates the pagination of [MessageEmbed]s to be displayed in the [event] channel.
 *
 * When [showPageNumbers] is enabled, the footer of the embeds will be
 * prepended with a page indicator, ex "Page 1 of 5 • Footer content here".
 *
 * If [usePagePicker] is enabled, an additional "prompt" button
 * will prompt the user for a page number to jump to. While the
 * page number prompt is active, the buttons become unresponsive.
 *
 * You may choose a different page to show first by providing a [currentPage].
 * If the index is out of bounds, an IllegalStateException will be thrown while initializing.
 *
 * If [text] is non-null, it will be displayed with each page as the message content.
 */
class Paginator(
    private val event: CommandEvent,
    private val showPageNumbers: Boolean = true,
    private val usePagePicker: Boolean = true,
    private var currentPage: Int = 0,
    private val text: String? = null
) {

    private val messages = mutableListOf<Message>()
    private var messageId: Long = 0

    /**
     * Initialize the paginator with the provided [pages].
     * Throws [IllegalArgumentException] if the list is
     * empty or the paginator was already initialized.
     */
    fun paginate(pages: List<MessageEmbed>) {
        if (pages.isEmpty()) throw IllegalArgumentException("Pages must not be empty")
        initialize(pages)
    }

    @Synchronized
    private fun initialize(pages: List<MessageEmbed>) {
        if (messages.isNotEmpty())
            throw IllegalStateException("Paginator has already been initialized")
        if (currentPage !in pages.indices)
            throw IllegalStateException("Starting page $currentPage must be within ${pages.indices}")
        if (event.isFromGuild) ensurePermissions(event, *requiredPermissions)
        renderMessages(pages) // The rendered pages will be added to the messages list

        // Figure out which buttons we need in this message
        val buttons = mutableListOf<Button>()
        if (messages.size > 1) {
            buttons.add(previousButton.withDisabled(currentPage == 0))
            if (usePagePicker) buttons.add(promptButton)
            buttons.add(nextButton.withDisabled(currentPage == messages.lastIndex))
        }
        buttons.add(destroyButton)

        // Finally send the paginator message as a reply
        event.message.reply(messages[currentPage])
            .setActionRows(ActionRow.of(buttons)).queue {
                messageId = it.idLong
                waitForButton()
            }
    }

    private fun renderMessages(pages: List<MessageEmbed>) {
        var pageNumber = 1
        for (page in pages) {
            // Only render the page indicators if there are multiple pages
            val embed = if (showPageNumbers && pages.size > 1) {
                val pageIndicator = "Page ${pageNumber++} of ${pages.size}"
                val newText = page.footer?.text?.let { "$pageIndicator • $it" } ?: pageIndicator
                EmbedBuilder(page).setFooter(newText, page.footer?.iconUrl).build()
            } else page
            messages.add(MessageBuilder().setContent(text).setEmbeds(embed).build())
        }
    }

    private fun waitForButton() {
        event.sandra.eventWaiter.waitForEvent(
            ButtonClickEvent::class, timeout = 1, unit = TimeUnit.MINUTES,
            expired = { event.channel.deleteMessageById(messageId).queue(null, handler) },
            test = { verifyButton(it) }
        ) { handleButton(it) }
    }

    private fun verifyButton(buttonEvent: ButtonClickEvent): Boolean {
        // Never process a button event that doesn't belong to our message
        if (buttonEvent.message?.idLong != messageId) return false
        // If the user is not the author, only acknowledge it
        return if (event.author.idLong != buttonEvent.user.idLong) {
            buttonEvent.deferEdit().queue()
            false
        } else when (buttonEvent.componentId) {
            // Verify the button that was clicked was actually enabled
            previousButtonId -> currentPage > 0
            nextButtonId -> currentPage < messages.lastIndex
            promptButtonId -> usePagePicker
            destroyButtonId -> true
            else -> false
        }
    }

    private fun handleButton(buttonEvent: ButtonClickEvent) {
        when (buttonEvent.componentId) {
            // Only send the prompt if we have permissions to
            promptButtonId -> if (event.isFromGuild) {
                if (hasPermissions(event, *requiredPermissions)) {
                    doPageSelection(buttonEvent)
                } else buttonEvent.deferEdit().queue { waitForButton() }
            } else doPageSelection(buttonEvent)
            // To destroy, just delete our message and escape the recursion
            destroyButtonId -> event.channel.deleteMessageById(messageId).queue(null, handler)
            else -> { // Only these buttons immediately change the page
                val previousPage = currentPage
                when (buttonEvent.componentId) {
                    previousButtonId -> if (currentPage > 0) currentPage--
                    nextButtonId -> if (currentPage < messages.lastIndex) currentPage++
                }
                if (previousPage != currentPage) updateMessage(buttonEvent) else waitForButton()
            }
        }
    }

    private fun updateMessage(buttonEvent: ButtonClickEvent) {
        // The message will never be ephemeral
        val buttons = buttonEvent.message!!.buttons
        // Update the buttons depending on the current page
        val previousIndex = buttons.indexOfFirst { it.id == previousButtonId }
        buttons[previousIndex] = buttons[previousIndex].withDisabled(currentPage == 0)
        val nextIndex = buttons.indexOfFirst { it.id == nextButtonId }
        buttons[nextIndex] = buttons[nextIndex].withDisabled(currentPage == messages.lastIndex)
        // Update the message either way, even if the event was acknowledged
        val action = if (buttonEvent.isAcknowledged) {
            event.channel.editMessageById(messageId, messages[currentPage]).setActionRow(buttons)
        } else buttonEvent.editMessage(messages[currentPage]).setActionRow(buttons)
        action.queue { waitForButton() }
    }

    private fun doPageSelection(buttonEvent: ButtonClickEvent) {
        // Acknowledge that the prompt button was clicked
        buttonEvent.deferEdit().queue()
        // Prompt the user for the page number to jump to
        argumentAction<Long>(
            event, event.translate("general.page_prompt", false), ArgumentType.DIGIT, { waitForButton() }
        ) { digit ->
            val index = digit.toInt() - 1
            if (index in messages.indices && index != currentPage) {
                currentPage = index
                updateMessage(buttonEvent)
            } else waitForButton()
        }
    }

    companion object {
        private const val promptButtonId = "paginator:prompt"
        private const val previousButtonId = "paginator:previous"
        private const val nextButtonId = "paginator:next"
        private const val destroyButtonId = "paginator:destroy"

        private val promptButton = Button.secondary(promptButtonId, Emoji.fromMarkdown(Emotes.PROMPT))
        private val previousButton = Button.primary(previousButtonId, Emoji.fromMarkdown(Emotes.PREVIOUS))
        private val nextButton = Button.primary(nextButtonId, Emoji.fromMarkdown(Emotes.NEXT))
        private val destroyButton = Button.danger(destroyButtonId, Emoji.fromMarkdown(Emotes.CLEAR))

        // We can't do much if the message was deleted externally, so we just ignore it
        private val handler = ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)

        private val requiredPermissions = arrayOf(
            Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_HISTORY
        )
    }

}
