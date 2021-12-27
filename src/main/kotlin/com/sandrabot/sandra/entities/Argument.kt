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

package com.sandrabot.sandra.entities

import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.exceptions.MissingArgumentException
import com.sandrabot.sandra.utils.spaceRegex
import io.ktor.http.cio.websocket.*
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import java.util.*
import kotlin.time.Duration

private val tokenRegex = Regex("""\[(@)?(?:([A-z]+):)?([A-z]+)(?::([A-z0-9,.]+))?]""")
private val durationRegex = Regex("""^(?!$)(?:(\d+)d ?)?(?:(\d+)h ?)?(?:(\d+)m ?)?(?:(\d+)s)?$""")
private val emoteRegex = Regex("""<a?:\S{2,32}:(\d{17,19})>""")

/**
 * Represents an object that may be parsed from text and consumed by a command as an argument.
 * The internal constructor ensures the rules defined in [compileArguments] are enforced.
 */
class Argument internal constructor(
    val name: String, val type: ArgumentType, val isRequired: Boolean, val options: List<*>
) {
    val usage = run {
        val (start, end) = if (isRequired) "<" to ">" else "[" to "]"
        "$start$name$end"
    }

    override fun toString(): String {
        val required = if (isRequired) "@" else ""
        val size = if (options.isNotEmpty()) "[${options.size}]" else ""
        return "A:$required$name($type)$size"
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
    val arguments = LinkedList<Argument>()
    for (match in tokenRegex.findAll(tokens)) {
        val (text, atSign, rawName, rawType, rawOptions) = match.groupValues
        val isRequired = atSign.isNotEmpty()
        // This will throw if the type is invalid
        val type = ArgumentType.fromName(rawType)

        // Figure out if options are applicable and validate them
        val options: List<*> = if (rawOptions.isEmpty()) emptyList<String>() else when (type) {
            // Options can only be used with TEXT, INTEGER, and DOUBLE types
            ArgumentType.TEXT, ArgumentType.INTEGER, ArgumentType.DOUBLE ->
                // Split by commas with a limit of 25 distinct entries
                rawOptions.lowercase().split(',').distinct().take(25).let { list ->
                    when (type) {
                        ArgumentType.TEXT -> list
                        ArgumentType.INTEGER -> list.map { int -> int.toLongOrNull() }
                        ArgumentType.DOUBLE -> list.map { num -> num.toDoubleOrNull() }
                        else -> throw AssertionError("Illegal option type")
                    }.also { mappedList ->
                        // Throw if any options couldn't be converted
                        if (null in mappedList) throw IllegalArgumentException("Illegal option type for argument type $type")
                    }
                }
            // Throw if options are defined on any other argument type
            else -> throw IllegalArgumentException("Argument type $type cannot use options")
        }

        // The name must always be lowercase
        val name = rawName.ifEmpty { type.name }.lowercase()

        // Arguments must have distinct names, throw if there are duplicates
        if (arguments.any { name == it.name })
            throw IllegalArgumentException("Duplicate argument name $name in $text at ${match.range}")

        // Required arguments must be listed first
        if (isRequired && arguments.any { !it.isRequired })
            throw IllegalArgumentException("Required argument $text at ${match.range} must be listed before other arguments")

        arguments.add(Argument(name, type, isRequired, options))
    }
    return arguments
}

/**
 * Parses the arguments provided by the user into objects easily consumed by commands.
 */
fun parseArguments(event: CommandEvent, arguments: List<Argument>): ArgumentResult {
    val parsedArguments = mutableMapOf<String, Any>()
    val options = event.options

    for (arg in arguments) {
        val result: Any? = when (arg.type) {
            ArgumentType.BOOLEAN -> findOption(options, arg)?.asBoolean
            ArgumentType.DOUBLE -> findOption(options, arg)?.asDouble
            ArgumentType.INTEGER -> findOption(options, arg)?.asLong
            ArgumentType.MENTIONABLE -> findOption(options, arg)?.asMentionable
            ArgumentType.ROLE -> findOption(options, arg)?.asRole
            ArgumentType.TEXT -> findOption(options, arg)?.asString
            ArgumentType.USER -> findOption(options, arg)?.asUser

            ArgumentType.CHANNEL -> parseChannels<TextChannel>(options, arg)
            ArgumentType.NEWS -> parseChannels<NewsChannel>(options, arg)
            ArgumentType.STAGE -> parseChannels<StageChannel>(options, arg)
            ArgumentType.VOICE -> parseChannels<VoiceChannel>(options, arg)

            ArgumentType.CATEGORY -> parseCategory(options, arg)
            ArgumentType.COMMAND -> parseCommand(event, options, arg)
            ArgumentType.DURATION -> parseDuration(options, arg)
            ArgumentType.EMOTE -> parseEmote(event, options, arg)
        }
        // Add the parsed values to the map if anything was found
        if (result != null) parsedArguments[arg.name] = result
    }

    // Check for any required arguments that failed to parse
    arguments.firstOrNull { it.isRequired && it.name !in parsedArguments }
        ?.let { throw MissingArgumentException(event, it) }

    return ArgumentResult(parsedArguments)
}

/* ==================== Parsing Methods ===================== */

private fun findOption(options: List<OptionMapping>, arg: Argument): OptionMapping? =
    options.firstOrNull { arg.name == it.name && arg.type.optionType == it.type }

private inline fun <reified T : GuildChannel> parseChannels(options: List<OptionMapping>, arg: Argument): T? =
    findOption(options, arg)?.asGuildChannel?.let { if (it is T) it else null }

private fun parseCategory(options: List<OptionMapping>, arg: Argument): Category? = findOption(options, arg)?.let {
    Category.values().firstOrNull { category -> category.name == it.asString.uppercase() }
}

private fun parseCommand(event: CommandEvent, options: List<OptionMapping>, arg: Argument): Command? =
    findOption(options, arg)?.let { event.sandra.commands[it.asString.replace(spaceRegex, "/")] }

private fun parseDuration(options: List<OptionMapping>, arg: Argument): Duration? {
    val option = findOption(options, arg) ?: return null
    // Due to regex negative lookahead we need to match the entire string
    val match = durationRegex.matchEntire(option.asString) ?: return null
    // The time units must be in order so that makes this easy enough
    val (_, days, hours, minutes, seconds) = match.groupValues.map { it.ifBlank { "0" } }
    // This is sadly the most straightforward way to parse the duration, as
    // the spaces are required when parsing but optional in the regex
    val duration = Duration.parseOrNull("${days}d ${hours}h ${minutes}m ${seconds}s")
    return duration?.let { if (it.inWholeSeconds > 0) duration else null }
}

private fun parseEmote(event: CommandEvent, options: List<OptionMapping>, arg: Argument): Emote? {
    val option = findOption(options, arg) ?: return null
    // Only match the entire string to be consistent with the other types
    val match = emoteRegex.matchEntire(option.asString) ?: return null
    // Check against all guilds, usually the emote isn't from the calling guild
    return event.sandra.shards.getEmoteById(match.groupValues[1])
}
