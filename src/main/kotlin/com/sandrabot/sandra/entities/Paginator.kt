/*
 * Copyright 2017-2020 Avery Clifton and Logan Devecka
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
import com.sandrabot.sandra.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Facilitates the pagination of [MessageEmbed]s to be displayed in the [event] context.
 *
 * When [showPageNumbers] is enabled, the footer of the pages will be
 * prepended with a page indicator, ex "Page 1 of 5 • Footer content here".
 *
 * If [usePagePicker] is enabled, an additional "hashtag" reaction
 * will prompt the context for a page number to jump to. While the
 * page number prompt is active, the reactions become unresponsive.
 *
 * You may choose a [startingPage] by setting the index of the page to display first.
 * If the index is out of bounds, an IllegalStateException will be thrown while initializing.
 *
 * If [text] is non-null, it will be displayed with each page as the message content.
 */
class Paginator(
    private val event: CommandEvent,
    private val showPageNumbers: Boolean = true,
    private val usePagePicker: Boolean = true,
    startingPage: Int = 0,
    private val text: String? = null
) {

    private val messages = mutableListOf<Message>()
    private val scope = CoroutineScope(EmptyCoroutineContext)

    private var currentPage = startingPage
    private var cancel: (() -> Unit)? = null
    private var select: ((Message) -> Unit)? = null

    /**
     * When set, an additional "x" reaction will be displayed.
     * If the context selects the reaction, the paginator will exit and invoke [newCancel].
     * You may pass `null` to clear this field.
     */
    fun onCancel(newCancel: (() -> Unit)?): Paginator = also { cancel = newCancel }

    /**
     * When set, an additional checkmark reaction will be displayed.
     * If the context selects the reaction, the paginator will exit an invoke [newSelect].
     * You may pass `null` to clear this field.
     */
    fun onSelect(newSelect: ((Message) -> Unit)?): Paginator = also { select = newSelect }

    /**
     * Initialize the paginator with the provided [pages].
     * Throws IllegalArgumentException if the list is empty.
     */
    fun paginate(pages: List<MessageEmbed>) {
        if (pages.isEmpty()) throw IllegalArgumentException("Pages must not be empty")
        initialize(pages)
    }

    @Synchronized
    private fun initialize(pages: List<MessageEmbed>) {
        if (messages.isNotEmpty())
            throw IllegalStateException("Paginator has already been initialized")
        if (currentPage < 0 || currentPage > pages.lastIndex)
            throw IllegalStateException("Starting page must be between ${pages.indices}")
        if (event.isFromGuild) ensurePermissions(event, *requiredPermissions)

        renderMessages(pages)

        scope.launch {
            val sentMessage = event.message.reply(messages[currentPage]).await()

            if (select != null && usePagePicker && messages.size > 1)
                sentMessage.addReaction(Emotes.HASHTAG.asReaction()).await()
            if (messages.size > 1) sentMessage.addReaction(Emotes.PREVIOUS.asReaction()).await()

            if (select != null) sentMessage.addReaction(Emotes.SUCCESS.asReaction()).await()
            else if (usePagePicker && messages.size > 1) sentMessage.addReaction(Emotes.HASHTAG.asReaction()).await()

            if (messages.size > 1) sentMessage.addReaction(Emotes.NEXT.asReaction()).await()
            if (cancel != null) sentMessage.addReaction(Emotes.ERROR.asReaction()).await()

            waitForReaction(sentMessage)
        }
    }

    private fun renderMessages(pages: List<MessageEmbed>) {
        var pageNumber = 1
        for (page in pages) {
            // Only render the page indicators if there are multiple pages
            val embed = if (showPageNumbers && pages.size > 1) {
                val builder = EmbedBuilder(page)
                val (text, iconUrl) = page.footer?.text to page.footer?.iconUrl
                val pageIndicator = "Page ${pageNumber++} of ${pages.size}"
                builder.setFooter(text?.let { "$pageIndicator • $it" } ?: pageIndicator, iconUrl).build()
            } else page
            val builder = MessageBuilder()
            builder.setContent(text).setEmbed(embed)
            messages.add(builder.build())
        }
    }

    private fun waitForReaction(message: Message) {
        event.sandra.eventWaiter.waitForEvent(
            GenericMessageReactionEvent::class,
            timeout = 1, unit = TimeUnit.MINUTES,
            expired = { handleExpire(message) },
            test = { verifyReaction(it, message) }
        ) { handleReaction(it, message) }
    }

    private fun handleExpire(message: Message) {
        if (cancel == null && event.isFromGuild) {
            if (hasPermissions(event, Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE)) {
                message.clearReactions().queue()
            }
        } else if (cancel != null) {
            message.delete().queue()
            cancel?.invoke()
        }
    }

    private fun verifyReaction(reactionEvent: GenericMessageReactionEvent, message: Message): Boolean {
        if (reactionEvent.messageId == message.id && reactionEvent.user == event.author) {
            val emote = reactionEvent.reactionEmote
            return if (emote.isEmote) when (emote.emote.asMention) {
                Emotes.HASHTAG -> usePagePicker
                Emotes.ERROR -> cancel != null
                Emotes.SUCCESS -> select != null
                Emotes.PREVIOUS, Emotes.NEXT -> true
                else -> false
            } else false
        }
        return false
    }

    private fun handleReaction(reactionEvent: GenericMessageReactionEvent, message: Message) {
        val previousPage = currentPage
        // When the reaction is verified, it is guaranteed to be an emote
        when (reactionEvent.reactionEmote.emote.asMention) {
            Emotes.PREVIOUS -> if (currentPage > 0) currentPage--
            Emotes.NEXT -> if (currentPage < messages.lastIndex) currentPage++
            Emotes.HASHTAG -> {
                if (event.isFromGuild) {
                    if (hasPermissions(event, Permission.MESSAGE_WRITE, Permission.MESSAGE_EXT_EMOJI)) {
                        doPageSelection(message)
                    } else waitForReaction(message)
                } else doPageSelection(message)
                return
            }
            Emotes.ERROR -> {
                message.delete().queue()
                cancel?.invoke()
                return
            }
            Emotes.SUCCESS -> {
                message.delete().queue()
                select?.invoke(message)
                return
            }
        }
        if (previousPage != currentPage) updateMessage(message) else waitForReaction(message)
    }

    private fun updateMessage(message: Message) {
        message.editMessage(messages[currentPage]).reference(event.message).queue(::waitForReaction)
    }

    private fun doPageSelection(message: Message) {
        if (hasPermissions(event, Permission.MESSAGE_WRITE, Permission.MESSAGE_EXT_EMOJI)) argumentAction<Long>(
            event, event.translate("general.page_prompt"), ArgumentType.DIGIT, { waitForReaction(message) }
        ) { digit ->
            val index = digit.toInt() - 1
            if (index in messages.indices && index != currentPage) {
                currentPage = index
                updateMessage(message)
            } else waitForReaction(message)
        } else waitForReaction(message)
    }

    companion object {
        private val requiredPermissions = arrayOf(
            // Base permissions that we still need to check for
            Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_EXT_EMOJI,
            // Additional permissions so the paginator can function
            Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY
        )
    }

}
