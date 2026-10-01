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

import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.HTTP_CLIENT
import dev.minn.jda.ktx.messages.MessageCreate
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("unused")
class Dog : Command() {

    override suspend fun execute(event: CommandEvent) {

        // defer the interaction response while making another request
        event.deferReply().queue()

        // send a request to the api for a random dog image
        val response = withContext(Dispatchers.IO) {
            HTTP_CLIENT.get("https://random.dog/woof")
        }

        // make sure the response returned ok
        if (response.status == HttpStatusCode.OK) {
            // respond to the interaction with the dog image
            event.sendMessage(MessageCreate(useComponentsV2 = true) {
                // read the body of the response as a plain file name
                val dogUrl = response.bodyAsText()
                mediaGallery { item("https://random.dog/$dogUrl") }
                // display a footer with an attribution to the website
                text("-# ${event.get("footnote")}")
            }).queue() // not ephemeral, dogs are too cute to hide
        } else event.sendInteractionError().queue()

    }

}
