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

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.io.InputStream
import kotlin.time.Duration

private val digitRegex = Regex("""\d+""")
private val doubleRegex = Regex("""[,.]""")
private val decimalRegex = Regex("""(\d+\.\d{2})\d+(.*)""")
val spaceRegex = Regex("""\s+""")

fun String.escape(single: Boolean = true): String = MarkdownSanitizer.escape(this, single)
fun String.sanitize(): String = MarkdownSanitizer.sanitize(this)
fun String.removeExtraSpaces(): String = replace(spaceRegex, " ").trim()
fun String.splitSpaces(limit: Int = 0): List<String> = split(spaceRegex, limit)
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

fun User.probableLocale(): DiscordLocale =
    mutualGuilds.groupingBy { it.locale }.eachCount().maxByOrNull { it.value }?.key ?: DiscordLocale.ENGLISH_US

fun <T> useResourceStream(path: String, block: InputStream.() -> T): T =
    object {}.javaClass.classLoader.getResourceAsStream(path)?.use(block)
        ?: throw IllegalArgumentException("Unable to load resource: $path")
