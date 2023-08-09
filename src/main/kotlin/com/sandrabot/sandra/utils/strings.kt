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

import com.sandrabot.sandra.constants.Constants
import kotlinx.serialization.json.JsonObject
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.io.InputStream
import java.util.*
import kotlin.time.Duration

private val digitRegex = Regex("""\d+""")
private val doubleRegex = Regex("""[,.]""")
private val decimalRegex = Regex("""(\d+\.\d{2})\d+(.*)""")
private val emoteRegex = Regex("""<a?:\S{2,32}:(\d{17,19})>""")
val spaceRegex = Regex("""\s+""")

fun String.asEmoteUrl() = "https://cdn.discordapp.com/emojis/${emoteRegex.find(this)?.groupValues?.get(1)}.png"
fun String.asReaction(): String = this.substring(1, lastIndex)

fun String.sanitize(): String = MarkdownSanitizer.sanitize(this)
fun String.removeExtraSpaces(): String = this.replace(spaceRegex, " ").trim()
fun String.splitSpaces(limit: Int = 0): List<String> = this.split(spaceRegex, limit)
fun String.capitalizeWords(): String = split(" ").joinToString {
    it.lowercase().replaceFirstChar { ch -> ch.uppercase() }
}

fun String.truncate(maxLength: Int) = if (length > maxLength) {
    substring(0, maxLength - 3).substringBeforeLast(' ') + "..."
} else this

fun User.format(): String = "**${effectiveName.sanitize()}**"
fun Number.format(): String = "**%,d**".format(this).replace(",", "**,**")
fun Double.format(): String = "**%,.2f**".format(this).replace(doubleRegex, "**$0**")
fun Duration.format(): String = toString().replace(decimalRegex, "$1$2").replace(digitRegex, "**$0**")

fun DiscordLocale.toLocale(): Locale = Locale.forLanguageTag(locale)
fun User.probableLocale(): DiscordLocale =
    mutualGuilds.groupingBy { it.locale }.eachCount().maxByOrNull { it.value }?.key ?: DiscordLocale.ENGLISH_US

fun <T> useResourceStream(path: String, block: InputStream.() -> T): T =
    object {}.javaClass.classLoader.getResourceAsStream(path)?.use(block)
        ?: throw IllegalArgumentException("Unable to load resource: $path")

fun hastebin(text: String): String? = try {
    postBlocking<JsonObject>("${Constants.HASTEBIN}/documents", text).let {
        "${Constants.HASTEBIN}/${it.string("key")}"
    }
} catch (t: Throwable) {
    null
}
