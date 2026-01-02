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

package com.sandrabot.sandra.commands.`fun`

import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.utils.HTTP_CLIENT
import dev.minn.jda.ktx.messages.MessageCreate
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Suppress("unused")
class Cat : Command() {

    override suspend fun execute(event: CommandEvent) = withContext(Dispatchers.IO) {

        event.deferReply().queue()
        val response = HTTP_CLIENT.get("https://cataas.com/cat?json=true")
        val jsonBody = response.body<JsonObject>()
        val catUrl = jsonBody["url"]?.jsonPrimitive?.content
        if (response.status == HttpStatusCode.OK && catUrl != null) {
            event.sendMessage(MessageCreate(useComponentsV2 = true) {
                mediaGallery { item(catUrl) }
                val catTags = jsonBody["tags"]?.jsonArray?.map { it.jsonPrimitive.content }
                val actualTags = catTags?.takeUnless { it.isEmpty() }?.take(3)?.joinToString()
                val tags = if (actualTags.isNullOrBlank()) "" else " ${Unicode.BULLET} $actualTags"
                text("-# ${event.get("footnote") + tags}")
            }).queue()
        } else event.sendError(event.getAny("core.interaction_error")).asEphemeral().queue()

    }

}
