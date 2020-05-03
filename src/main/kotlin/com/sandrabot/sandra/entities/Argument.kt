/*
 *    Copyright 2017-2020 Avery Clifton and Logan Devecka
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sandrabot.sandra.entities

import java.util.*

/**
 * Represents an object that may be parsed from text and consumed by a command as an argument.
 * The private constructor ensures the rules defined in [compile] are enforced.
 */
class Argument private constructor(val name: String, val type: ArgumentType, val isRequired: Boolean) {

    override fun toString(): String {
        val required = if (isRequired) "@" else ""
        return "A:$required$name($type)"
    }

    /**
     * Proxy object presented to commands for consumption.
     */
    data class ArgumentHolder(val type: ArgumentType, val value: Any) {

        override fun toString(): String {
            return "AH:$type($value)"
        }

    }

    companion object {

        private val tokenPattern = Regex("""\[(@)?(?:([A-z]+):)?([A-z]+)]""")

        /**
         * Compiles the provided [tokens] into a read-only list of arguments.
         *
         * Tokens must meet these requirements otherwise an [IllegalArgumentException] will be thrown:
         *  * Tokens must not evaluate to [ArgumentType.UNKNOWN]
         *  * Tokens with the type [ArgumentType.FLAG] must not be required
         *  * Tokens must not share names
         *
         * Tokens must follow the format `[@name:type]`. Only the brackets and type are required.
         * Any text that does not follow the correct pattern will be ignored by this method.
         * If a name isn't supplied, it will default to the name of the type.
         * If multiple tokens have the same type, they must be named separately
         * to differentiate between which token is being referred to.
         * All letters are case in-sensitive, however the name will always be converted to lowercase.
         *  * The `@` denotes the token as a required argument.
         *  * The name can be used to describe the argument in usage prompts.
         *    If the name is present, the colon must also be present to separate the name and type.
         *  * The type is the name of any entry in the [ArgumentType] class.
         *
         * Required arguments are used by the command system to halt execution if the argument is missing.
         * Commands may assume required arguments will always be present.
         *
         * Examples:
         *  * `[text]` - Optional argument with the name of "text" and the type of [ArgumentType.TEXT]
         *  * `[@time:duration]` - Required argument with the name of "time" and the type of [ArgumentType.DURATION]
         *  * `[bots:flag]` - Optional argument with the name of "bots" and the type of [ArgumentType.FLAG]
         *  * `[yourmom:isgay]` - Throws [IllegalArgumentException], the type is invalid
         *  * `[@global:flag]` - Throws [IllegalArgumentException], flags must not be required
         *  * `[time:digit] [time:duration]` - Throws [IllegalArgumentException], the name is already used
         */
        fun compile(tokens: String): List<Argument> {
            val arguments = LinkedList<Argument>()
            for (match in tokenPattern.findAll(tokens)) {
                val (text, atSign, rawName, rawType) = match.groupValues
                val isRequired = atSign.isNotEmpty()

                val type = ArgumentType.fromName(rawType)
                if (type == ArgumentType.UNKNOWN) {
                    // Unknown arguments are not permitted
                    throw IllegalArgumentException("Unknown argument type in $text at ${match.range}")
                } else if (type == ArgumentType.FLAG && isRequired) {
                    // Flag arguments cannot be required
                    throw IllegalArgumentException("Flag argument cannot be required in $text at ${match.range}")
                }

                // The name must always be lowercase
                val name = (if (rawName.isEmpty()) type.name else rawName).toLowerCase()

                // Arguments cannot share names, if there are two
                // of the same type they must be named differently
                if (arguments.any { name.equals(it.name, ignoreCase = true) }) {
                    throw IllegalArgumentException("Argument already exists with the name in $text at ${match.range}")
                }

                arguments.add(Argument(name, type, isRequired))
            }
            // Convert the arguments to a read-only list
            return arguments.toTypedArray().asList()
        }

    }

}
