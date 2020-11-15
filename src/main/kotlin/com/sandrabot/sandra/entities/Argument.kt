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

package com.sandrabot.sandra.entities

import com.sandrabot.sandra.entities.Argument.Companion.compile
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.exceptions.MissingArgumentException
import com.sandrabot.sandra.utils.removeExtraSpaces
import com.sandrabot.sandra.utils.splitSpaces
import me.xdrop.fuzzywuzzy.FuzzySearch
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.VoiceChannel
import java.util.*

/**
 * Represents an object that may be parsed from text and consumed by a command as an argument.
 * The private constructor ensures the rules defined in [compile] are enforced.
 */
class Argument private constructor(
        val name: String, val type: ArgumentType, val isRequired: Boolean, val isArray: Boolean
) {

    override fun toString(): String {
        val required = if (isRequired) "@" else ""
        val array = if (isArray) "*" else ""
        return "A:$required$name($type)$array"
    }

    companion object {

        private val tokenRegex = Regex("""\[(@)?(?:([A-z]+):)?([A-z]+)(\*)?]""")
        private val durationRegex = Regex("""^(?!$)(?:(\d+)d)?(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?$""")
        private val userRegex = Regex("""<@!?(\d{17,19})>""")
        private val channelRegex = Regex("""<#(\d{17,19})>""")
        private val emoteRegex = Regex("""<a?:\S{2,32}:(\d{17,19})>""")
        private val roleRegex = Regex("""<@&(\d{17,19})>""")
        private val digitRegex = Regex("""\d{1,16}""")
        private val flagRegex = Regex("""!(\S+)""")
        private val idRegex = Regex("""\d{17,19}""")

        /**
         * Compiles the provided [tokens] into a read-only list of arguments.
         *
         * Tokens must meet these requirements otherwise an [IllegalArgumentException] will be thrown:
         *  * Tokens must not evaluate to [ArgumentType.UNKNOWN]
         *  * Tokens with the type [ArgumentType.FLAG] must not be required
         *  * Tokens with the type [ArgumentType.FLAG] must not be arrays
         *  * Tokens with the type [ArgumentType.TEXT] must not be arrays
         *  * Tokens must not share names
         *
         * Tokens must follow the format `[@name:type*]`. Only the brackets and type are required.
         * Any text that does not follow the correct pattern will be ignored by this method.
         * If a name isn't supplied, it will default to the name of the type.
         * If multiple tokens have the same type, they must be named differently
         *  to indicate clearly which token is being referred to.
         * All letters are case in-sensitive, however the name will always be converted to lowercase.
         *  * The `@` denotes the token as a required argument.
         *  * The name can be used to describe the argument in usage prompts.
         *    If the name is present, the colon must also be present to separate the name and type.
         *  * The type is the name of any entry in the [ArgumentType] enum.
         *  * The `*` denotes the token as an array, where multiple results may be parsed.
         *
         * Required arguments are used by the command system to halt execution if the argument is missing.
         * Commands may assume required arguments will always be present.
         *
         * Examples:
         *  * `[text]` - Optional argument with the name of "text" and the type of [ArgumentType.TEXT]
         *  * `[@time:duration]` - Required argument with the name of "time" and the type of [ArgumentType.DURATION]
         *  * `[bots:flag]` - Optional argument with the name of "bots" and the type of [ArgumentType.FLAG]
         *  * `[users:user*]` - Optional array of arguments with the name of "users" and the type of [ArgumentType.USER]
         *  * `[yourmom:isgay]` - Throws [IllegalArgumentException], the type is invalid
         *  * `[@channel*]` - Throws [IllegalArgumentException], flags must not be arrays
         *  * `[text*]` - Throws [IllegalArgumentException], text must not be arrays
         *  * `[@global:flag]` - Throws [IllegalArgumentException], flags must not be required
         *  * `[time:digit] [time:duration]` - Throws [IllegalArgumentException], the name is already used
         */
        @Suppress("KDocUnresolvedReference")
        fun compile(tokens: String): List<Argument> {
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
                val name = (if (rawName.isEmpty()) type.name else rawName).toLowerCase()

                // Arguments cannot share names, if there are two
                // of the same type they must be named differently
                if (arguments.any { name.equals(it.name, ignoreCase = true) }) {
                    throw IllegalArgumentException("Argument already exists with the name in $text at ${match.range}")
                }

                arguments.add(Argument(name, type, isRequired, isArray))
            }
            // Convert the arguments to a read-only list
            return arguments.toTypedArray().asList()
        }

        /**
         * Extracts a single argument from the command context.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> singleton(event: CommandEvent, type: ArgumentType): T? {
            val argument = Argument("singleton", type, isRequired = false, isArray = false)
            return parse(event, listOf(argument)).results["singleton"] as T?
        }

        /**
         * Parses the arguments provided by a user into the requested argument types.
         */
        fun parse(event: CommandEvent, arguments: List<Argument>): ArgumentResult {
            val parsedValues = mutableMapOf<String, Any>()
            var remainingText = event.args

            for (arg in arguments) {

                // There's nothing left to parse
                if (remainingText.isEmpty()) break

                val result: List<Any>? = when (arg.type) {

                    ArgumentType.USER, ArgumentType.CHANNEL, ArgumentType.EMOTE -> {
                        // Figure out which regex pattern we are going to need
                        val regex = when (arg.type) {
                            ArgumentType.USER -> userRegex
                            ArgumentType.CHANNEL -> channelRegex
                            ArgumentType.EMOTE -> emoteRegex
                            else -> throw AssertionError()
                        }
                        // Figure out which list is relevant to us
                        val mentioned = when (arg.type) {
                            ArgumentType.USER -> event.message.mentionedUsers
                            ArgumentType.CHANNEL -> event.message.mentionedChannels
                            ArgumentType.EMOTE -> event.message.emotes
                            else -> throw AssertionError()
                        }
                        // Attempt to parse any entity mentions first
                        val matches = LinkedList<IMentionable>()
                        if (mentioned.isNotEmpty()) do {
                            // If there are no more entity mentions we can stop looking
                            val mention = regex.find(remainingText) ?: break
                            remainingText = remainingText.removeRange(mention.range).removeExtraSpaces()
                            matches.add(mentioned.first { it.id == mention.groupValues[1] })
                        } while (arg.isArray)
                        // We will only support additional lookup by id for users, this keeps things simple
                        // If we didn't find anything, maybe try looking for ids
                        if (arg.type == ArgumentType.USER && (matches.isEmpty() || arg.isArray)) {
                            // We need to find all matches to prevent traversing them forever
                            for (match in idRegex.findAll(remainingText)) {
                                // If no user exists with this id try the next match
                                val user = event.sandra.completeUser(match.value) ?: continue
                                remainingText = remainingText.replaceFirst(match.value, "").removeExtraSpaces()
                                matches.add(user)
                                if (!arg.isArray) break
                            }
                        }
                        matches
                    }

                    ArgumentType.ROLE -> {
                        // Attempt to parse any role mentions first
                        val mentioned = event.message.mentionedRoles
                        val roles = LinkedList<Role>()
                        if (mentioned.isNotEmpty()) do {
                            // If there are no more role mentions we can stop looking
                            val mention = roleRegex.find(remainingText) ?: break
                            remainingText = remainingText.removeRange(mention.range).removeExtraSpaces()
                            roles.add(mentioned.first { it.id == mention.groupValues[1] })
                        } while (arg.isArray)
                        if (roles.isEmpty() || arg.isArray) {
                            // Try looking for role ids if we didn't find anything
                            for (match in idRegex.findAll(remainingText)) {
                                // Keep looking if we don't find a role with this id
                                val role = event.guild.getRoleById(match.value) ?: continue
                                remainingText = remainingText.replaceFirst(match.value, "").removeExtraSpaces()
                                roles.add(role)
                                if (!arg.isArray) break
                            }
                        }
                        // Search the roles if we still didn't find anything
                        if (roles.isEmpty() || arg.isArray) {
                            // Roles are special in that they are one of two
                            // discord entities that we allow be looked up by name
                            remainingText = fuzzySearch(event.guild.roles, roles, remainingText) { it.name }
                        }
                        roles
                    }

                    ArgumentType.DURATION -> {
                        val matches = LinkedList<Long>()
                        // Due to how regex negative lookahead works we need to search using words
                        for (split in remainingText.splitSpaces()) {
                            // If this word doesn't match we can try the next one
                            val match = durationRegex.matchEntire(split) ?: continue
                            // The duration units must be in order, so this is easy enough
                            val (_, days, hours, minutes, seconds) = match.groupValues
                            var secondsTotal = 0L
                            // Add up all the different units into seconds
                            // This time we can actually handle invalid numbers correctly
                            if (days.isNotEmpty()) days.toIntOrNull()?.let { secondsTotal += 86400 * it }
                            if (hours.isNotEmpty()) hours.toIntOrNull()?.let { secondsTotal += 3600 * it }
                            if (minutes.isNotEmpty()) minutes.toIntOrNull()?.let { secondsTotal += 60 * it }
                            if (seconds.isNotEmpty()) seconds.toIntOrNull()?.let { secondsTotal += it }
                            // Only if this duration is actually useful do we use it
                            if (secondsTotal >= 0) {
                                remainingText = remainingText.replaceFirst(split, "").removeExtraSpaces()
                                matches.add(secondsTotal)
                                if (!arg.isArray) break
                            }
                        }
                        matches
                    }

                    ArgumentType.DIGIT -> {
                        val matches = LinkedList<Long>()
                        // Usually you will want to place this after other arguments
                        // This will literally match any numbers in the remaining text
                        // However it will not match any ids since the max length here is 16
                        for (split in remainingText.splitSpaces()) {
                            val match = digitRegex.matchEntire(split) ?: break
                            remainingText = remainingText.removeRange(match.range).removeExtraSpaces()
                            // Only keep the match if it can actually fit into a long
                            match.value.toLongOrNull()?.let { matches.add(it) }
                            if (!arg.isArray) break
                        }
                        matches
                    }

                    ArgumentType.FLAG -> {
                        // Since flags can't be arrays, they are one
                        // of the only arguments that can't return a list
                        // This is fine since the singleton will be extracted later
                        var isPresent = false
                        for (match in flagRegex.findAll(remainingText)) {
                            // If the argument name matches then it is considered present
                            if (match.groupValues[1].equals(arg.name, ignoreCase = true)) {
                                remainingText = remainingText.removeRange(match.range).removeExtraSpaces()
                                isPresent = true
                                break
                            }
                        }
                        listOf(isPresent)
                    }

                    ArgumentType.COMMAND -> {
                        val commands = LinkedList<Command>()
                        // We probably won't support fuzzy searching for
                        // commands, it would be too ambiguous with aliases
                        val remainingSplit = remainingText.splitSpaces()
                        for (split in remainingSplit) {
                            // If this word isn't a command name, try the next one
                            val command = event.sandra.commands.getCommand(split) ?: continue
                            remainingText = remainingText.replaceFirst(split, "").removeExtraSpaces()
                            commands.add(command)
                            if (!arg.isArray) break
                        }
                        commands
                    }

                    ArgumentType.VOICE -> {
                        // Voice channels are also special, they cannot be
                        // mentioned easily and must be looked up by name
                        val voiceChannels = LinkedList<VoiceChannel>()
                        val channelList = event.guild.voiceChannels
                        // First check any channel ids, this is more accurate than names
                        for (match in idRegex.findAll(remainingText)) {
                            // Keep looking if we don't find a channel with this id
                            val channel = event.guild.getVoiceChannelById(match.value) ?: continue
                            remainingText = remainingText.replaceFirst(match.value, "").removeExtraSpaces()
                            voiceChannels.add(channel)
                            if (!arg.isArray) break
                        }
                        // Search the channels if we didn't find anything
                        if (voiceChannels.isEmpty() || arg.isArray) {
                            remainingText = fuzzySearch(channelList, voiceChannels, remainingText) { it.name }
                        }
                        voiceChannels
                    }

                    ArgumentType.TEXT -> {
                        // Text is another exception to arrays as it can only return a singleton
                        // This argument must be put at the very end of your tokens
                        // It literally consumes the rest of the remaining text, as you can see
                        val remainingTemp = remainingText
                        remainingText = ""
                        listOf(remainingTemp)
                    }

                    ArgumentType.ITEM -> TODO()
                    ArgumentType.UNKNOWN -> throw AssertionError()

                }

                if (result != null && result.isNotEmpty()) {
                    parsedValues[arg.name] = if (arg.isArray) result.distinct() else result.first()
                }

            }

            // Check for any missing arguments
            val missing = arguments.firstOrNull { it.isRequired && it.name !in parsedValues }
            if (missing != null) throw MissingArgumentException(event, missing)

            return ArgumentResult(parsedValues)
        }

        private fun <T> fuzzySearch(sourceList: List<T>, resultList: LinkedList<T>,
                                    remainingText: String, transform: (T) -> String): String {
            var newRemaining = remainingText
            // To do the name lookup efficiently, we can use fuzzy searching with a cutoff for accuracy
            val sorted = FuzzySearch.extractSorted(remainingText, sourceList.map(transform), 80)
            if (sorted.isNotEmpty()) {
                // Find the entities we found based on the result index
                val sortedEntities = sorted.map { sourceList[it.index] }
                // Make a string from the names of the entities
                val entityWords = sortedEntities.joinToString(" ", transform = transform)
                // Search for all the words that might match the entity names
                val topWords = FuzzySearch.extractSorted(entityWords, remainingText.splitSpaces(), 50).map { it.string }
                // Remove those words from the remaining text
                // This can potentially remove words that were not used, but it should be fine
                topWords.forEach { newRemaining = remainingText.replaceFirst(it, "").removeExtraSpaces() }
                resultList.addAll(sortedEntities)
            }
            return newRemaining
        }

    }

}
