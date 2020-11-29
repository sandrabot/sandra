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

package com.sandrabot.sandra.utils

import com.beust.klaxon.Klaxon
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Locale
import com.sandrabot.sandra.entities.SandraGuild
import com.sandrabot.sandra.entities.SandraUser
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.io.StringReader
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private val spaceRegex = Regex("""\s+""")

fun String.asReaction(): String = this.substring(1, lastIndex)

fun String.sanitize(): String = MarkdownSanitizer.sanitize(this)
fun String.removeExtraSpaces(): String = this.replace(spaceRegex, " ").trim()
fun String.splitSpaces(limit: Int = 0): List<String> = this.split(spaceRegex, limit)

fun Number.format(): String = "**%,d**".format(this).replace(",", "**,**")

@ExperimentalTime
fun duration(duration: Duration): String = duration.toComponents { days, hours, minutes, seconds, nanoseconds ->
    val builder = StringBuilder()
    if (days > 0) builder.append(days.format()).append("d")
    if (hours > 0) {
        if (builder.isNotEmpty()) builder.append(" ")
        builder.append("**").append(hours).append("**h")
    }
    if (minutes > 0) {
        if (builder.isNotEmpty()) builder.append(" ")
        builder.append("**").append(minutes).append("**m")
    }
    if (seconds > 0) {
        if (builder.isNotEmpty()) builder.append(" ")
        builder.append("**").append(seconds).append("**s")
    }
    val milliseconds = nanoseconds / 1000000
    if (milliseconds > 0) {
        if (builder.isNotEmpty()) builder.append(" ")
        builder.append("**").append(milliseconds).append("**ms")
    }
    return builder.toString()
}

fun findLocale(sandraGuild: SandraGuild, sandraUser: SandraUser): Locale {
    return sandraUser.locale ?: sandraGuild.locale ?: Locale.ENGLISH
}

fun hastebin(text: String): String? {
    val response = HttpUtil.post("${Constants.HASTEBIN}/documents", text)
    return if (response.isEmpty()) null else try {
        val key = Klaxon().parseJsonObject(StringReader(response)).string("key")
        "${Constants.HASTEBIN}/$key"
    } catch (e: Exception) {
        null
    }
}
