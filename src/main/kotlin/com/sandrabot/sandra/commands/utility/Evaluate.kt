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

package com.sandrabot.sandra.commands.utility

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.managers.CommandManager
import com.sandrabot.sandra.managers.ConfigurationManager
import com.sandrabot.sandra.managers.RedisManager
import com.sandrabot.sandra.utils.await
import com.sandrabot.sandra.utils.duration
import com.sandrabot.sandra.utils.hastebin
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.sharding.ShardManager
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Suppress("unused")
class Evaluate : Command(name = "eval", guildOnly = true, ownerOnly = true) {

    private val engineContext = SupervisorJob() + Dispatchers.IO
    private val engine = KotlinJsr223JvmLocalScriptEngineFactory().scriptEngine
    private val imports: String

    init {
        setIdeaIoUseFallback()
        val importBuilder = StringBuilder()
        // Import every sandra package, excluding commands
        Package.getPackages().map { it.name }.filter {
            it.startsWith("com.sandrabot.sandra") && !it.contains("commands")
        }.forEach { importBuilder.append("import ").append(it).append(".*\n") }
        // Include some miscellaneous stuff for quality of life
        listOf(
            "java.awt.Color", "java.util.*", "java.util.concurrent.TimeUnit",
            "java.time.OffsetDateTime", "kotlin.coroutines.*", "kotlinx.coroutines.*",
            "net.dv8tion.jda.api.*", "net.dv8tion.jda.api.entities.*", "redis.clients.jedis.*"
        ).forEach { importBuilder.append("import $it\n") }
        imports = importBuilder.append("\n\n").toString()
    }

    @ExperimentalTime
    override suspend fun execute(event: CommandEvent) {

        if (event.args.isEmpty()) {
            event.reply("you forgot to include the script")
            return
        }

        // Create a map of the variables we might want
        val bindings = listOf(
            Triple("event", event, CommandEvent::class),
            Triple("author", event.author, User::class),
            Triple("member", event.member, Member::class),
            Triple("channel", event.textChannel, TextChannel::class),
            Triple("guild", event.guild, Guild::class),
            Triple("id", event.guild.id, String::class),
            Triple("gc", event.guildConfig, GuildConfig::class),
            Triple("uc", event.userConfig, UserConfig::class),
            Triple("sandra", event.sandra, Sandra::class),
            Triple("shards", event.sandra.shards, ShardManager::class),
            Triple("commands", event.sandra.commands, CommandManager::class),
            Triple("redis", event.sandra.redis, RedisManager::class),
            Triple("config", event.sandra.config, ConfigurationManager::class),
        )

        // Insert them into the script engine
        bindings.forEach { engine.put(it.first, it.second) }

        // Prepend the variables to the script so we don't have to use bindings["name"]
        val variables = bindings.joinToString("\n", postfix = "\n\n") {
            """val ${it.first} = bindings["${it.first}"] as ${it.third.qualifiedName}"""
        }

        // Strip the command block if present
        val strippedScript = blockPattern.find(event.args)?.let { it.groupValues[1] } ?: event.args

        // Before we begin constructing the script, we need to find any additional imports
        val importLines = strippedScript.lines().takeWhile { it.startsWith("import") }
        val additionalImports = importLines.joinToString("\n", postfix = "\n\n")
        val script = strippedScript.lines().filterNot { it in importLines }.joinToString("\n")

        // Wait for the message to send so we can edit it later
        val message = event.channel.sendMessage("${Emotes.SPIN} hold on while i crunch the numbers...").await()
        val handler = CoroutineExceptionHandler { _, throwable ->
            handleResult(message, "**unknown** with unhandled exception", throwable.stackTraceToString())
        }

        // Measure how long it takes to evaluate the script
        val timedResult = measureTimedValue {
            // We still need to catch any unexpected coroutine exceptions
            withContext(engineContext + handler) {
                try {
                    engine.eval(imports + additionalImports + variables + script)
                } catch (throwable: Throwable) {
                    throwable
                }
            }
        }

        val duration = duration(timedResult.duration)
        val result = timedResult.value?.toString() ?: run {
            message.editMessage("evaluated in $duration with no returns").queue()
            return
        }
        handleResult(message, duration, result)

    }

    @ExperimentalTime
    private fun handleResult(message: Message, duration: String, result: String) {
        // Check the result length, if it's too large attempt to upload it to hastebin
        val formatted = "evaluated in $duration\n```\n$result\n```"
        if (formatted.length > 2000) {
            // If the upload failed print it to stdout
            val hastebin = hastebin(result) ?: run {
                message.editMessage("upload failed, result was logged").queue()
                logger.info("Evaluation result failed to upload:\n$result")
                return
            }
            message.editMessage("evaluated in $duration $hastebin").queue()
        } else message.editMessage(formatted).queue()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Evaluate::class.java)
        private val blockPattern = Regex("""```\S*\n(.*)```""", RegexOption.DOT_MATCHES_ALL)
    }

}
