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

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.lastfm.ImageHolder
import com.sandrabot.sandra.entities.lastfm.ImageSize
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.User
import java.awt.Color
import java.io.InputStream
import javax.imageio.ImageIO

suspend fun ImageHolder.tryAverageColor(size: ImageSize): Color? = withContext(Dispatchers.IO) {
    try {
        val url = getImageUrl(size) ?: return@withContext null
        val stream = HTTP_CLIENT.get(url).body<InputStream>()
        findTrueAverageColor(ImageIO.read(stream))
    } catch (_: Exception) {
        // unable to decode the image
        null
    }
}

fun CommandEvent.verifyLastUser(): Pair<User, String>? {
    val targetUser = arguments.user() ?: user
    // make sure the user has entered their last.fm username
    val username = sandra.config[targetUser].lastUsername ?: run {
        val key = if (targetUser == user) "missing_username" else "missing_other"
        replyEmoji(Emotes.LASTFM, getAny("core.lastfm.$key", targetUser)).asEphemeral().queue()
        return null
    }
    return Pair(targetUser, username)
}
