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
import com.sandrabot.sandra.utils.removeExtraSpaces
import com.sandrabot.sandra.utils.splitSpaces
import me.xdrop.fuzzywuzzy.FuzzySearch
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.VoiceChannel
import java.util.*
import java.util.concurrent.atomic.AtomicReference

private val tokenRegex = Regex("""\[(@)?(?:([A-z]+):)?([A-z]+)(\*)?]""")
private val durationRegex = Regex("""^(?!$)(?:(\d+)d)?(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?$""")
private val wordRegex = Regex(""""([^"]+?)"|^ *?([^"\s]+)""")
private val userRegex = Regex("""<@!?(\d{17,19})>""")
private val channelRegex = Regex("""<#(\d{17,19})>""")
private val emoteRegex = Regex("""<a?:\S{2,32}:(\d{17,19})>""")
private val roleRegex = Regex("""<@&(\d{17,19})>""")
private val digitRegex = Regex("""\d{1,16}""")
private val idRegex = Regex("""\d{17,19}""")
private val flagRegex = Regex("""!(\S+)""")

/**
 * Represents an object that may be parsed from text and consumed by a command as an argument.
 * The internal constructor ensures the rules defined in [compileArguments] are enforced.
 */
class Argument internal constructor(
    val name: String, val type: ArgumentType, val isRequired: Boolean, val isArray: Boolean
) {
    val usage = run {
        val (start, end) = if (isRequired) "<" to ">" else "[" to "]"
        val array = if (isArray) "..." else ""
        "$start$name$array$end"
    }

    override fun toString(): String {
        val required = if (isRequired) "@" else ""
        val array = if (isArray) "*" else ""
        return "A:$required$name($type)$array"
    }
}

/**
 * Compiles the provided token string into a list of arguments.
 *
 * Tokens must follow the format `[@name:type*]`. Only the brackets and type are required.
 * Any text that does not follow the correct pattern will be ignored by this method.
 * If a name isn't supplied, it will default to the name of the type.
 * If multiple tokens have the same type, they must be named differently
 *  to indicate clearly which token is being referred to.
 * All letters are case in-sensitive, however the name will always be converted to lowercase.
 *
 *  * The `@` denotes the token as a required argument.
 *  * The name can be used to describe the argument in usage prompts.
 *    If the name is present, the colon must also be present to separate the name and type.
 *  * The type is the name of any entry in the [ArgumentType] enum.
 *  * The `*` denotes the token as an array, where multiple results may be parsed.
 *
 * Required arguments are used by the command system to halt execution if the argument is missing.
 * Commands may assume required arguments will always be present.
 *
 * Tokens must meet these requirements otherwise an [IllegalArgumentException] will be thrown:
 *  * Tokens must not evaluate to [ArgumentType.UNKNOWN]
 *  * Tokens with the type [ArgumentType.FLAG] must not be required
 *  * Tokens with the type [ArgumentType.FLAG] must not be arrays
 *  * Tokens with the type [ArgumentType.TEXT] must not be arrays
 *  * Tokens must not share names
 *
 * Examples:
 *  * `[text]` - Optional argument with the name of "text" and the type of [ArgumentType.TEXT]
 *  * `[@time:duration]` - Required argument with the name of "time" and the type of [ArgumentType.DURATION]
 *  * `[bots:flag]` - Optional argument with the name of "bots" and the type of [ArgumentType.FLAG]
 *  * `[users:user*]` - Optional array of arguments with the name of "users" and the type of [ArgumentType.USER]
 *  * `[yourmom:isgay]` - Throws [IllegalArgumentException], the type is invalid
 *  * `[text*]` - Throws [IllegalArgumentException], text must not be arrays
 *  * `[@global:flag]` - Throws [IllegalArgumentException], flags must not be required
 *  * `[time:digit] [time:duration]` - Throws [IllegalArgumentException], the name is already used
 */
@Suppress("KDocUnresolvedReference")
fun compileArguments(tokens: String): List<Argument> {
    val arguments = LinkedList<Argument>()
    for (match in tokenRegex.findAll(tokens)) {
        val (text, atSign, rawName, rawType, asterisk) = match.groupValues
        val isRequired = atSign.isNotEmpty()
        val isArray = asterisk.isNotEmpty()

        val type = ArgumentType.fromName(rawType)
        if (type == ArgumentType.UNKNOWN) {
            // Unknown arguments are not permitted
            throw IllegalArgumentException("Unknown argument type in $text at ${match.range}")
        } else if (type == ArgumentType.FLAG && isRequired) {
            // Flag arguments cannot be required
            throw IllegalArgumentException("Flag arguments cannot be required in $text at ${match.range}")
        } else if (type == ArgumentType.FLAG && isArray) {
            // Flag arguments cannot be arrays
            throw IllegalArgumentException("Flag arguments cannot be arrays in $text at ${match.range}")
        } else if (type == ArgumentType.TEXT && isArray) {
            // Text arguments cannot be arrays
            throw IllegalArgumentException("Text arguments cannot be arrays in $text at ${match.range}")
        }

        // The name must always be lowercase
        val name = rawName.ifEmpty { type.name }.toLowerCase()

        // Arguments cannot share names, if there are two
        // of the same type they must be named differently
        if (arguments.any { name.equals(it.name, ignoreCase = true) }) {
            throw IllegalArgumentException("Duplicate argument name $name in $text at ${match.range}")
        }
        arguments.add(Argument(name, type, isRequired, isArray))
    }
    return arguments
}

/**
 * Extracts a single argument from the command context.
 */
@Suppress("UNCHECKED_CAST")
fun <T> singleton(event: CommandEvent, args: String, type: ArgumentType): T? {
    val argument = Argument("singleton", type, isRequired = false, isArray = false)
    return parseArguments(listOf(argument), event, args).results["singleton"] as T?
}

/**
 * Parses the arguments provided by the user into objects easily consumed by commands.
 */
fun parseArguments(arguments: List<Argument>, event: CommandEvent, args: String): ArgumentResult {
    val parsedArguments = mutableMapOf<String, Any>()
    val remaining = AtomicReference(args)

    for (arg in arguments) {
        if (remaining.get().isBlank()) break
        val result: List<Any> = when (arg.type) {
            ArgumentType.USER, ArgumentType.CHANNEL, ArgumentType.EMOTE,
            ArgumentType.ROLE, ArgumentType.VOICE -> parseSnowflake(event, remaining, arg)
            ArgumentType.COMMAND -> parseCommand(event, remaining, arg)
            ArgumentType.DURATION -> parseDuration(remaining, arg)
            ArgumentType.DIGIT -> parseDigit(remaining, arg)
            ArgumentType.FLAG -> parseFlag(remaining, arg)
            ArgumentType.WORD -> parseWord(remaining, arg)
            ArgumentType.TEXT -> parseText(remaining)
            else -> throw AssertionError("No parsing implemented for argument type ${arg.type}")
        }

        // Add the parsed values to the map if anything was found
        if (result.isNotEmpty()) parsedArguments[arg.name] = if (arg.isArray) result else result.first()
    }

    // Check for and enforce any missing required arguments
    arguments.firstOrNull { it.isRequired && it.name !in parsedArguments }
        ?.let { throw MissingArgumentException(event, it) }

    return ArgumentResult(parsedArguments)
}

/* ========== Parsing Methods =========== */

/**
 * Parses argument types USER, CHANNEL, VOICE, ROLE, and EMOTE
 */
private fun parseSnowflake(
    event: CommandEvent, remaining: AtomicReference<String>, argument: Argument
): List<IMentionable> {
    val parsedValues = mutableListOf<IMentionable>()
    // Figure out which regex pattern and list we are going
    // to need since this method can parse multiple types
    val (regex, snowflakes) = when (argument.type) {
        ArgumentType.USER -> userRegex to event.message.mentionedUsers
        ArgumentType.CHANNEL -> channelRegex to event.message.mentionedChannels
        ArgumentType.VOICE -> channelRegex to event.guild.voiceChannels
        ArgumentType.ROLE -> roleRegex to event.message.mentionedRoles
        ArgumentType.EMOTE -> emoteRegex to event.message.emotes
        else -> throw AssertionError("Invalid type ${argument.type} for mentionable parsing")
    }
    // First look for any mentions that match the regex for the type
    if (snowflakes.isNotEmpty()) for (match in regex.findAll(remaining.get())) {
        // Add the match to the parsed values if the id matches a snowflake of the requested type
        snowflakes.firstOrNull { match.groupValues[1] == it.id }?.also { parsedValues.add(it) } ?: continue
        // If we reach this line, the mention was validated and we need to remove the matched text
        remaining.getAndUpdate { it.replaceFirst(match.value, "").removeExtraSpaces() }
        if (!argument.isArray) break
    }
    // If no mentions were found or this is an array, additionally search for ids
    if (parsedValues.isEmpty() || argument.isArray) {
        // Find all of the ids at once to properly skip invalid matches
        for (match in idRegex.findAll(remaining.get())) {
            // Validate that the id found is the same type as the requested type
            when (argument.type) {
                // User lookup is a blocking call, use only within coroutines
                ArgumentType.USER -> event.sandra.completeUser(match.value)
                ArgumentType.CHANNEL -> event.guild.getTextChannelById(match.value)
                ArgumentType.VOICE -> event.guild.getVoiceChannelById(match.value)
                ArgumentType.ROLE -> event.guild.getRoleById(match.value)
                ArgumentType.EMOTE -> event.guild.getEmoteById(match.value)
                else -> throw AssertionError("you should never see this")
            }?.also { parsedValues.add(it) } ?: continue
            // If we reach this line, the id was validated and we need to remove the matched text
            remaining.getAndUpdate { it.replaceFirst(match.value, "").removeExtraSpaces() }
            if (!argument.isArray) break
        }
    }
    // Fuzzy searching is only used as a last resort for roles and voice channels
    if (parsedValues.isEmpty() || argument.isArray) {
        if (argument.type == ArgumentType.ROLE || argument.type == ArgumentType.VOICE) do {
            // To search lists while tolerating misspellings we can use fuzzy searching
            // First we need to find a word or phrase to search with, instead of matching the entire remaining
            val match = wordRegex.find(remaining.get()) ?: break
            // Since this regex has two groups we need to find the one that was matched
            val word = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: break
            when (argument.type) {
                // Only extract the top result or break if none match with >= 80% accuracy
                ArgumentType.ROLE -> FuzzySearch.extractTop(word, event.guild.roles, Role::getName, 85)
                ArgumentType.VOICE -> FuzzySearch.extractTop(word, event.guild.voiceChannels, VoiceChannel::getName, 85)
                else -> throw AssertionError("you should never see this")
            }.firstOrNull()?.also { parsedValues.add(it.referent) } ?: break
            // Update remaining by removing the word we used if a result was found
            remaining.getAndUpdate { it.removeRange(match.range).removeExtraSpaces() }
        } while (argument.isArray)
    }
    return parsedValues.distinct()
}

/**
 * Parses argument type DURATION
 */
private fun parseDuration(remaining: AtomicReference<String>, argument: Argument): List<Long> {
    val parsedValues = mutableListOf<Long>()
    // Due to regex negative lookahead we need to search using words
    // This also enforces a rule that time units must be in order from days to seconds
    for (word in remaining.get().splitSpaces()) {
        var durationInSeconds = 0L
        val match = durationRegex.matchEntire(word) ?: continue
        // The time units are already in order so that makes this easy enough
        val (_, days, hours, minutes, seconds) = match.groupValues
        if (days.isNotEmpty()) days.toIntOrNull()?.let { durationInSeconds += 86400 * it }
        if (hours.isNotEmpty()) hours.toIntOrNull()?.let { durationInSeconds += 3600 * it }
        if (minutes.isNotEmpty()) minutes.toIntOrNull()?.let { durationInSeconds += 60 * it }
        if (seconds.isNotEmpty()) seconds.toIntOrNull()?.let { durationInSeconds += it }
        // Remove any durations that were matched, but don't use any that are zero
        remaining.getAndUpdate { it.replaceFirst(word, "").removeExtraSpaces() }
        if (durationInSeconds >= 0) {
            parsedValues.add(durationInSeconds)
            if (!argument.isArray) break
        }
    }
    return parsedValues
}

/**
 * Parses argument type DIGIT
 */
private fun parseDigit(remaining: AtomicReference<String>, argument: Argument): List<Long> {
    val parsedValues = mutableListOf<Long>()
    // Only match entire words, we don't want to pick digits from just anywhere
    for (word in remaining.get().splitSpaces()) {
        val match = digitRegex.matchEntire(word) ?: continue
        // Only keep the match if it can actually fit into a long
        match.value.toLongOrNull()?.also { parsedValues.add(it) } ?: continue
        remaining.getAndUpdate { it.replaceFirst(match.value, "").removeExtraSpaces() }
        if (!argument.isArray) break
    }
    return parsedValues
}

/**
 * Parses argument type FLAG
 */
private fun parseFlag(remaining: AtomicReference<String>, argument: Argument): List<Boolean> {
    // Flags cannot be required nor arrays, this method will always return a singleton
    var isFlagPresent = false
    // Make sure a flag isn't found in the middle of a word
    for (word in remaining.get().splitSpaces()) {
        val match = flagRegex.matchEntire(word) ?: continue
        // If the flag name matches the argument name it is considered present
        if (match.groupValues[1].equals(argument.name, ignoreCase = true)) {
            remaining.getAndUpdate { it.replaceFirst(match.value, "").removeExtraSpaces() }
            isFlagPresent = true
            break
        }
    }
    return listOf(isFlagPresent)
}

/**
 * Parses argument type COMMAND
 */
private fun parseCommand(event: CommandEvent, remaining: AtomicReference<String>, argument: Argument): List<Command> {
    val parsedValues = mutableListOf<Command>()
    // We don't support fuzzy searching commands because it would be too ambiguous with aliases
    for (word in remaining.get().splitSpaces()) {
        event.sandra.commands.getCommand(word)?.also { parsedValues.add(it) } ?: continue
        // If we reach this line, that means a command was found
        remaining.getAndUpdate { it.replaceFirst(word, "").removeExtraSpaces() }
        if (!argument.isArray) break
    }
    return parsedValues
}

/**
 * Parses argument type WORD
 */
private fun parseWord(remaining: AtomicReference<String>, argument: Argument): List<String> {
    // Words are considered any characters before the first space, or characters within quotes
    val parsedValues = mutableListOf<String>()
    do {
        // Find the first word or any phrase within quotes in the remaining text
        val match = wordRegex.find(remaining.get()) ?: break
        val (_, first, second) = match.groupValues
        // Since the regex can match single words or entire phrases,
        // we need to determine the group that was matched
        parsedValues.add(first.ifBlank { second })
        remaining.getAndUpdate { it.removeRange(match.range).removeExtraSpaces() }
    } while (argument.isArray)
    return parsedValues
}

/**
 * Parses argument type TEXT
 */
private fun parseText(remaining: AtomicReference<String>): List<String> {
    // Text is another exception to arrays as it can only return a singleton
    // You should probably put this at the very end of your tokens
    return listOf(remaining.getAndSet(""))
}
