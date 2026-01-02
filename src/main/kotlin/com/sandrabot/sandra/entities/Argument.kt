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

package com.sandrabot.sandra.entities

import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.exceptions.MissingArgumentException
import com.sandrabot.sandra.utils.spaceRegex
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.time.Duration

private val tokenRegex = Regex("""\[(@)?(?:([A-z]+):)?([A-z]+)(?::([A-z\d,.]+))?]""")
private val durationRegex = Regex("""^(?!$)(?:(\d+)d ?)?(?:(\d+)h ?)?(?:(\d+)m ?)?(?:(\d+)s)?$""")
private val emoteRegex = Regex("""<a?:\S{2,32}:(\d{17,19})>""")

/**
 * Represents an object that may be parsed from text and consumed by a command as an argument.
 * The internal constructor ensures the rules defined in [compileArguments] are enforced.
 */
class Argument internal constructor(
    val name: String, val type: ArgumentType, val isRequired: Boolean, val choices: List<*>,
) {
    val usage = if (isRequired) "<$name>" else "[$name]"

    override fun toString(): String {
        val required = if (isRequired) "@" else ""
        val size = if (choices.isNotEmpty()) "[${choices.size}]" else ""
        return "Argument:$required$name($type)$size"
    }
}

/**
 * Compiles the provided token string into a list of arguments.
 *
 * Tokens must follow the format `[@name:type:option,option]`. Only the brackets and type are required.
 * Any text that does not follow the correct pattern will be ignored by the compiler.
 * If a name isn't supplied, it will default to the name of the type.
 * If multiple tokens have the same type, they must be named differently
 *  to indicate clearly which token is being referred to.
 * All letters are case-insensitive, however the name and options will always be converted to lowercase.
 *
 *  * The `@` denotes the token as a required argument.
 *    * Required arguments must be listed first before other arguments.
 *  * The name can be used to describe the argument in usage prompts.
 *    * If the name is present, the colon must also be present to separate the name and type.
 *  * The type is the name of any entry in the [ArgumentType] enum.
 *  * The options are a comma delimited list of values a user may pick.
 *    * If not empty, these are the only possible values.
 *    * When command data is serialized, the name of the value will be pulled from locale data by index.
 *    * You may only define a maximum of 25 options due to a restriction applied by Discord.
 *    * The options must be of the same type as the expected argument.
 *    * Options may only be applied to [ArgumentType.TEXT], [ArgumentType.INTEGER], or [ArgumentType.DOUBLE].
 *
 * Required arguments are guaranteed to be present when a command is processed.
 * While Discord will not allow a required argument to be missing, it may not be resolved if the input is malformed.
 *
 * Tokens must meet these requirements, otherwise an [IllegalArgumentException] will be thrown:
 *  * Tokens must use a type listed in [ArgumentType]
 *  * Tokens must have distinct names
 *  * Tokens must not have invalid options
 *
 * Examples:
 *  * `[text]` - Optional argument with the name of "text" and the type of [ArgumentType.TEXT]
 *  * `[@time:duration]` - Required argument with the name of "time" and the type of [ArgumentType.DURATION]
 *  * `[bots:boolean]` - Optional argument with the name of "bots" and the type of [ArgumentType.BOOLEAN]
 *  * `[@days:integer:1,7]` - Required argument with the name "days", type of [ArgumentType.INTEGER], and options "1" or "7"
 *  * `[minutes:duration] [@reason:text]` - Throws [IllegalArgumentException], the required argument isn't first
 *  * `[something:else]` - Throws [IllegalArgumentException], the type "else" is invalid
 *  * `[time:integer] [time:duration]` - Throws [IllegalArgumentException], the name "time" is already used
 *  * `[quantity:integer:1.0,2.0]` - Throws [IllegalArgumentException], the option type is DOUBLE when INTEGER is required
 */
@Suppress("KDocUnresolvedReference")
fun compileArguments(tokens: String): List<Argument> {
    val arguments = mutableListOf<Argument>()
    tokenRegex.findAll(tokens).forEach { matchResult ->
        val (text, asterisk, rawName, rawType, rawOptions) = matchResult.groupValues
        val isRequired = asterisk.isNotEmpty()
        val type = ArgumentType.fromName(rawType)

        // figure out if options are applicable and validate them
        val options: List<*> = if (rawOptions.isEmpty()) emptyList<String>() else when (type.optionType) {
            // only argument types with text, integer, and double value types may use options
            OptionType.STRING, OptionType.INTEGER, OptionType.NUMBER -> rawOptions.lowercase().split(',').distinct()
                .take(25).let { list ->
                    when (type.optionType) {
                        OptionType.STRING -> list
                        OptionType.INTEGER -> list.map(String::toLongOrNull)
                        OptionType.NUMBER -> list.map(String::toDoubleOrNull)
                    }.also { mappedList ->
                        // throw if any options couldn't be converted
                        if (null in mappedList) throw IllegalArgumentException("Illegal option type in $text")
                    }
                }

            else -> throw IllegalArgumentException("Argument type $type cannot use options in $text")
        }

        // the name must always be lowercase
        val name = rawName.ifEmpty { type.name }.lowercase()
        // arguments must have distinct names, throw if there's any duplicates
        if (arguments.any { name == it.name }) throw IllegalArgumentException("Duplicate argument name already in use $text")

        // required arguments must be listed first
        if (isRequired && arguments.any { !it.isRequired }) throw IllegalArgumentException("Required argument $name must be listed before other arguments")

        arguments += Argument(name, type, isRequired, options)
    }
    return arguments
}

/**
 * Parses the provided [arguments] from the provided [event].
 */
fun parseArguments(event: CommandEvent, arguments: List<Argument>): ArgumentResult {
    val parsed = mutableMapOf<String, Any>()
    arguments.forEach { argument ->
        val value: Any? = when (argument.type) {
            // these types are simple and can be resolved directly
            ArgumentType.ATTACHMENT -> findOption(event, argument)?.asAttachment
            ArgumentType.BOOLEAN -> findOption(event, argument)?.asBoolean
            ArgumentType.DOUBLE -> findOption(event, argument)?.asDouble
            ArgumentType.INTEGER -> findOption(event, argument)?.asLong
            ArgumentType.MEMBER -> findOption(event, argument)?.asMember
            ArgumentType.MENTIONABLE -> findOption(event, argument)?.asMentionable
            ArgumentType.ROLE -> findOption(event, argument)?.asRole
            ArgumentType.TEXT -> findOption(event, argument)?.asString
            ArgumentType.USER -> findOption(event, argument)?.asUser

            // channels can just be filtered by type
            ArgumentType.CHANNEL -> parseChannels<TextChannel>(event, argument)
            ArgumentType.NEWS -> parseChannels<NewsChannel>(event, argument)
            ArgumentType.STAGE -> parseChannels<StageChannel>(event, argument)
            ArgumentType.VOICE -> parseChannels<VoiceChannel>(event, argument)

            // these types require more complex parsing
            ArgumentType.CATEGORY -> parseCategory(event, argument)
            ArgumentType.COMMAND -> parseCommand(event, argument)
            ArgumentType.DURATION -> parseDuration(event, argument)
            ArgumentType.EMOTE -> parseEmote(event, argument)
        }
        if (argument.isRequired && value == null) throw MissingArgumentException(event, argument)
        if (value != null) parsed[argument.name] = value
    }
    return ArgumentResult(parsed)
}


private fun findOption(event: CommandEvent, argument: Argument): OptionMapping? =
    event.options.firstOrNull { argument.name == it.name && argument.type.optionType == it.type }

private inline fun <reified T : GuildChannel> parseChannels(event: CommandEvent, argument: Argument): T? =
    findOption(event, argument)?.asChannel?.let { it as? T }

private fun parseCategory(event: CommandEvent, argument: Argument): Category? {
    val option = findOption(event, argument) ?: return null
    // allow the categories to be searched by their localized names
    val categories = Category.entries.associateBy { event.getAny("core.categories.${it.displayName}") }
    // since we don't support fuzzy searching, the option is the key
    return categories[option.asString.lowercase()]
}

private fun parseCommand(event: CommandEvent, argument: Argument): Command? {
    val option = findOption(event, argument) ?: return null
    // all commands can be searched by their localized names
    val commands = event.sandra.commands.values.associateBy { command ->
        if (command.isSubcommand) command.path.split('.').runningReduce { a, b -> "$a.$b" }
            .joinToString("/") { event.getAny("commands.$it.name") } else event.getAny("commands.${command.path}.name")
    }
    // since we don't support fuzzy searching, the option is the key
    return commands[option.asString.lowercase().replace(spaceRegex, "/")]
}

private fun parseDuration(event: CommandEvent, argument: Argument): Duration? {
    val option = findOption(event, argument) ?: return null
    // due to regex negative lookahead, we need to match the entire string
    val match = durationRegex.matchEntire(option.asString) ?: return null
    // the time units must be in order, so that makes this easy enough
    val (_, days, hours, minutes, seconds) = match.groupValues.map { it.ifBlank { "0" } }
    // this is sadly the most straightforward way to parse the duration, as
    // the spaces are required when parsing but optional in the regex
    val duration = Duration.parseOrNull("${days}d ${hours}h ${minutes}m ${seconds}s")
    return duration?.let { if (it.inWholeSeconds > 0) duration else null }
}

private fun parseEmote(event: CommandEvent, argument: Argument): RichCustomEmoji? {
    val option = findOption(event, argument) ?: return null
    // only match the entire string to be consistent with the other types
    val match = emoteRegex.matchEntire(option.asString) ?: return null
    // check against all guilds, usually the emote isn't from the calling guild
    return event.sandra.shards.getEmojiById(match.groupValues[1])
}
