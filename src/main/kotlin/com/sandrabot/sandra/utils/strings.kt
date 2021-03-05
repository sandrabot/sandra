/*
 * Copyright 2017-2021 Avery Carroll and Logan Devecka
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

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.Locale
import io.ktor.content.*
import io.ktor.http.*
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private val doubleRegex = Regex("""[,.]""")
private val emoteRegex = Regex("""<a?:\S{2,32}:(\d{17,19})>""")
private val spaceRegex = Regex("""\s+""")

fun String.asEmoteUrl() = "https://cdn.discordapp.com/emojis/${emoteRegex.find(this)?.groupValues?.get(1)}.png"
fun String.asReaction(): String = this.substring(1, lastIndex)

fun String.sanitize(): String = MarkdownSanitizer.sanitize(this)
fun String.removeExtraSpaces(): String = this.replace(spaceRegex, " ").trim()
fun String.splitSpaces(limit: Int = 0): List<String> = this.split(spaceRegex, limit)

fun User.format(): String = "**${name.sanitize()}**#**$discriminator**"
fun Number.format(): String = "**%,d**".format(this).replace(",", "**,**")
fun Double.format(): String = "**%,.2f**".format(this).replace(doubleRegex, "**$0**")

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

fun getPrefixUsed(sandra: Sandra, content: String, guild: Guild?): String? {
    val prefixes = if (guild != null) {
        val customPrefixes = sandra.config.getGuild(guild.idLong).prefixes
        sandra.commands.prefixes + customPrefixes.toTypedArray()
    } else sandra.commands.prefixes
    return prefixes.find { content.startsWith(it, ignoreCase = true) }
}

fun findLocale(guildConfig: GuildConfig, userConfig: UserConfig): Locale {
    return if (userConfig.locale == Locale.DEFAULT) {
        if (guildConfig.locale == Locale.DEFAULT) {
            Locale.DEFAULT
        } else guildConfig.locale
    } else userConfig.locale
}

fun hastebin(text: String): String? {
    val response = try {
        postBlocking<Map<String, String>>(
            "${Constants.HASTEBIN}/documents",
            TextContent(text, ContentType.Text.Plain)
        )
    } catch (t: Throwable) { null }
    return if (response == null) null else {
        "${Constants.HASTEBIN}/${response["key"]}"
    }
}
