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

package com.sandrabot.sandra.commands.`fun`

import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
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

    override suspend fun execute(event: CommandEvent) {

        // defer the interaction response while making another request
        event.deferReply().queue()

        // send a request to the api for a random cat image
        val response = withContext(Dispatchers.IO) {
            HTTP_CLIENT.get("https://cataas.com/cat?json=true")
        }

        // read the body of the response as a json object
        val jsonBody = response.body<JsonObject>()
        // attempt to retrieve the image url from the response
        val catUrl = jsonBody["url"]?.jsonPrimitive?.content

        // validate the response returned ok, and with a valid url
        if (response.status == HttpStatusCode.OK && catUrl != null) {
            // respond to the interaction with the cat image
            event.sendMessage(MessageCreate(useComponentsV2 = true) {
                mediaGallery { item(catUrl) }
                // display the tags in the footer after the attribution
                val catTags = jsonBody["tags"]?.jsonArray?.map { it.jsonPrimitive.content }
                // only show the first 3 tags, if they were provided
                val actualTags = catTags.takeUnless { it.isNullOrEmpty() }?.take(3)?.joinToString()
                val tags = if (actualTags.isNullOrBlank()) "" else " ${Unicode.BULLET} $actualTags"
                // append a footer with a link to the website and any tags
                text("-# ${event.get("footnote") + tags}")
            }).queue() // not ephemeral, cats are too cute to hide
        } else event.sendInteractionError().queue()

    }

}
