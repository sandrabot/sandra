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

import net.dv8tion.jda.api.utils.MarkdownSanitizer

private val spaceRegex = Regex("""\s+""")

fun asReaction(emote: String): String = emote.substring(1, emote.length - 1)

fun sanitize(sequence: String): String = MarkdownSanitizer.sanitize(sequence)

fun String.removeExtraSpaces(): String = this.replace(spaceRegex, " ").trim()
fun String.splitSpaces(): List<String> = this.split(spaceRegex)

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
