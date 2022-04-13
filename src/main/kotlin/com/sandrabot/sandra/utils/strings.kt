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

import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Locale
import kotlinx.serialization.json.JsonObject
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.MarkdownSanitizer
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

fun User.format(): String = "**${name.sanitize()}**#**$discriminator**"
fun Number.format(): String = "**%,d**".format(this).replace(",", "**,**")
fun Double.format(): String = "**%,.2f**".format(this).replace(doubleRegex, "**$0**")
fun Duration.format(): String = toString().replace(decimalRegex, "$1$2").replace(digitRegex, "**$0**")

fun getResourceAsText(path: String) = object {}.javaClass.getResource(path)?.readText()

fun findLocale(guildConfig: GuildConfig?, userConfig: UserConfig): Locale {
    return when {
        guildConfig == null -> userConfig.locale
        userConfig.locale != Locale.DEFAULT -> userConfig.locale
        else -> guildConfig.locale
    }
}

fun hastebin(text: String): String? = try {
    postBlocking<JsonObject>("${Constants.HASTEBIN}/documents", text).let {
        "${Constants.HASTEBIN}/${it.string("key")}"
    }
} catch (t: Throwable) {
    null
}
