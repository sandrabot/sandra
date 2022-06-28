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

package com.sandrabot.sandra.commands.`fun`

import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.await
import com.sandrabot.sandra.utils.httpClient
import com.sandrabot.sandra.utils.string
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

@Suppress("unused")
class Cat : Command(name = "cat") {

    override suspend fun execute(event: CommandEvent) = withContext(Dispatchers.IO) {
        event.deferReply(ephemeral = true).await()
        val response = httpClient.get("https://cataas.com/cat?json=true")
        if (response.status == HttpStatusCode.OK) {
            val url = response.body<JsonObject>().string("url")
            event.sendMessage("https://cataas.com$url").queue()
        } else event.sendError(event.translate("general.interaction_error", withRoot = false)).queue()
    }

}
