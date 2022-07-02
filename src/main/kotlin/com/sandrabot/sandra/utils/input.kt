/*
 * Copyright 2017-2022 Avery Carroll and Logan Devecka
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
import com.sandrabot.sandra.events.CommandEvent
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

suspend fun inputAction(
    commandEvent: CommandEvent, message: Message
): MessageReceivedEvent = commandEvent.sendMessage(message).await().let { promptMessage ->
    val messageEvent = commandEvent.sandra.shards.await<MessageReceivedEvent> { messageEvent ->
        commandEvent.user == messageEvent.author && commandEvent.channel == messageEvent.channel
    }
    promptMessage.delete().await()
    return messageEvent
}

suspend fun promptAction(event: CommandEvent, prompt: String, emote: String = Emotes.PROMPT) =
    inputAction(event, MessageBuilder("$emote $prompt").build())

suspend fun digitAction(event: CommandEvent, prompt: String) =
    promptAction(event, prompt).message.contentRaw.toLongOrNull()
