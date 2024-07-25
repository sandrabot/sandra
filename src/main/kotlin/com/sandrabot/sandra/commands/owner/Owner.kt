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

package com.sandrabot.sandra.commands.owner

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.events.asEphemeral
import com.sandrabot.sandra.utils.awardExperience
import com.sandrabot.sandra.utils.format
import dev.minn.jda.ktx.coroutines.await
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

@Suppress("unused")
class Owner : Command() {

    override suspend fun execute(event: CommandEvent) = Unit

    /**
     * Updates the avatar for the bot account that's currently signed in.
     */
    class Avatar : Command(arguments = "[image:attachment]") {
        override suspend fun execute(event: CommandEvent) {
            val attachment = event.arguments.attachment("image") ?: run {
                event.reply("missing attachment").queue()
                return
            }
            val image = attachment.proxy.downloadAsIcon().await()
            event.jda.selfUser.manager.setAvatar(image).flatMap { event.reply("that should do the trick!") }
                .onErrorFlatMap { event.reply("couldn't set the avatar; ${it.message}") }.queue()
        }
    }

    /**
     * Allows developers to fix level issues that may
     * occur after awarding large amounts of experience.
     */
    class Levels : Command(arguments = "[user]") {
        override suspend fun execute(event: CommandEvent) {
            val user = event.arguments.user() ?: event.user
            val config = event.sandra.config[user]
            val oldExperience = config.experience
            val oldLevel = config.level

            while (config.awardExperience(0)) {
                // all the magic already happens within the award method
            }

            val difference = (config.level - oldLevel).format()
            val expDifference = (oldExperience - config.experience).format()
            event.reply("user **${user.name}** gained $difference levels using $expDifference xp").queue()
        }
    }

    /**
     * This command enables the developers to terminate the application.
     */
    class Shutdown : Command() {
        override suspend fun execute(event: CommandEvent) {
            event.reply("goodbye :3").asEphemeral().await()
            exitProcess(0)
        }
    }

    /**
     * Enables debug mode by setting the logger level to DEBUG.
     */
    class Debug : Command(subgroup = "log") {
        override suspend fun execute(event: CommandEvent) {
            (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.DEBUG
            event.reply("debug logging enabled").asEphemeral().queue()
        }
    }

    /**
     * Represents a command that sets the logging level to INFO.
     */
    class Info : Command(subgroup = "log") {
        override suspend fun execute(event: CommandEvent) {
            (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.INFO
            event.reply("info logging enabled").asEphemeral().queue()
        }
    }

    /**
     * Represents a command that sets the logging level to TRACE.
     */
    class Trace : Command(subgroup = "log") {
        override suspend fun execute(event: CommandEvent) {
            (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.TRACE
            event.reply("trace logging enabled").asEphemeral().queue()
        }
    }


}
