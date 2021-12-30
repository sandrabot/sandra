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

package com.sandrabot.sandra.commands.owner

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.MemberConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.managers.CommandManager
import com.sandrabot.sandra.managers.ConfigurationManager
import com.sandrabot.sandra.managers.RedisManager
import com.sandrabot.sandra.utils.format
import com.sandrabot.sandra.utils.hastebin
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Suppress("unused")
class Evaluate : Command(name = "eval", arguments = "[@script:text]", guildOnly = true) {

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
            "java.time.OffsetDateTime", "kotlin.coroutines.*", "kotlinx.coroutines.*", "kotlin.time.Duration",
            "kotlinx.serialization.json.Json", "kotlinx.serialization.encodeToString", "kotlinx.serialization.decodeFromString",
            "net.dv8tion.jda.api.interactions.commands.*", "net.dv8tion.jda.api.interactions.commands.build.*",
            "net.dv8tion.jda.api.interactions.components.*", "net.dv8tion.jda.api.interactions.components.selections.*",
            "net.dv8tion.jda.api.*", "net.dv8tion.jda.api.entities.*", "redis.clients.jedis.*"
        ).forEach { importBuilder.append("import $it\n") }
        imports = importBuilder.append("\n\n").toString()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun execute(event: CommandEvent) {

        // Create a map of the variables we might want
        val bindings = listOf(
            Triple("event", event, CommandEvent::class),
            Triple("user", event.user, User::class),
            Triple("id", event.user.idLong, Long::class),
            Triple("member", event.member, Member::class),
            Triple("channel", event.textChannel, TextChannel::class),
            Triple("guild", event.guild, Guild::class),
            // This command can only be used within guilds, so guild will never be null
            Triple("gid", event.guild!!.idLong, Long::class),
            Triple("gc", event.guildConfig, GuildConfig::class),
            Triple("mc", event.memberConfig, MemberConfig::class),
            Triple("uc", event.userConfig, UserConfig::class),
            Triple("sandra", event.sandra, Sandra::class),
            Triple("shards", event.sandra.shards, ShardManager::class),
            Triple("commands", event.sandra.commands, CommandManager::class),
            Triple("redis", event.sandra.redis, RedisManager::class),
            Triple("config", event.sandra.config, ConfigurationManager::class),
        )

        // Insert them into the script engine
        bindings.forEach { engine.put(it.first, it.second) }

        // Prepend the variables to the script, so we don't have to use bindings["name"]
        val variables = bindings.joinToString("\n", postfix = "\n\n") {
            """val ${it.first} = bindings["${it.first}"] as ${it.third.qualifiedName}"""
        }

        // Strip the command block if present
        val args = event.arguments.text("script")!!
        val strippedScript = blockPattern.find(args)?.let { it.groupValues[1] } ?: args

        // Before we begin constructing the script, we need to find any additional imports
        val importLines = strippedScript.lines().takeWhile { it.startsWith("import") }
        val additionalImports = importLines.joinToString("\n", postfix = "\n\n")
        val script = strippedScript.lines().filterNot { it in importLines }.joinToString("\n")

        // Defer the reply so we can actually reply later
        event.deferReply().queue()
        val handler = CoroutineExceptionHandler { _, throwable ->
            handleResult(event, "**unknown** with unhandled exception", throwable.stackTraceToString())
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

        val duration = timedResult.duration.format()
        val result = timedResult.value?.toString() ?: run {
            event.sendMessage("evaluated in $duration with no returns").queue()
            return
        }
        handleResult(event, duration, result)

    }

    private fun handleResult(event: CommandEvent, duration: String, result: String) {
        // Check the result length, if it's too large attempt to upload it to hastebin
        val formatted = "evaluated in $duration\n```\n$result\n```"
        if (formatted.length > 2000) {
            // If the upload failed print it to stdout
            val hastebin = hastebin(result) ?: run {
                event.sendMessage("upload failed, result was logged").queue()
                logger.info("Evaluation result failed to upload:\n$result")
                return
            }
            event.sendMessage("evaluated in $duration $hastebin").queue()
        } else event.sendMessage(formatted).queue()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Evaluate::class.java)
        private val blockPattern = Regex("""```\S*\n(.*)```""", RegexOption.DOT_MATCHES_ALL)
    }

}
