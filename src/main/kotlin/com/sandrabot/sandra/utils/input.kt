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

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.events.CommandEvent
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

fun inputAction(
    event: CommandEvent, message: Message,
    timeout: Long = 2, unit: TimeUnit = TimeUnit.MINUTES,
    expired: (() -> Unit)? = null, consumer: (MessageReceivedEvent) -> Boolean
) = event.sendMessage(message).queue { promptMessage ->
    event.sandra.eventWaiter.waitForEvent(
        MessageReceivedEvent::class, timeout, unit,
        expired = {
            promptMessage.delete().queue()
            if (expired != null) expired()
        }, test = {
            it.author == event.user && it.channel == event.channel
        }, action = { messageEvent ->
            promptMessage.delete().queue()
            consumer(messageEvent).also { shouldDelete ->
                if (shouldDelete) checkAndDelete(event, messageEvent.message)
            }
        }
    )
}

fun promptAction(
    event: CommandEvent, prompt: String, emote: String = Emotes.PROMPT,
    timeout: Long = 2, unit: TimeUnit = TimeUnit.MINUTES,
    expired: (() -> Unit)? = null, consumer: (MessageReceivedEvent) -> Boolean
) {
    val message = MessageBuilder(emote + Unicode.VERTICAL_LINE + prompt).build()
    inputAction(event, message, timeout, unit, expired, consumer)
}

fun digitAction(
    event: CommandEvent, prompt: String, expired: (() -> Unit)? = null, consumer: (Long) -> Unit
) = promptAction(event, prompt, expired = expired) {
    it.message.contentRaw.toLongOrNull()?.let { digit -> consumer(digit); true } ?: run { expired?.invoke(); false }
}

private fun checkAndDelete(event: CommandEvent, message: Message) {
    if (event.isFromGuild && hasPermission(event, Permission.MESSAGE_MANAGE)) message.delete().queue()
}
