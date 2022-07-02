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
import com.sandrabot.sandra.config.ChannelConfig
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.MemberConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.managers.CommandManager
import com.sandrabot.sandra.managers.ConfigurationManager
import com.sandrabot.sandra.managers.RedisManager
import com.sandrabot.sandra.utils.format
import com.sandrabot.sandra.utils.hastebin
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
class Evaluate : Command(name = "evaluate", guildOnly = true) {

    private val engineContext = SupervisorJob() + Dispatchers.IO
    private val scriptEngine = KotlinJsr223JvmLocalScriptEngineFactory().scriptEngine
    private val persistentImports = mutableSetOf<String>()
    private var isRunning = false

    init {
        setIdeaIoUseFallback()
        persistentImports += Package.getPackages().map { it.name }.filter {
            it.startsWith("com.sandrabot.sandra") && !it.contains("commands")
        }.map { "$it.*" }.sorted()
        persistentImports += additionalImports.sorted()
    }

    override suspend fun execute(event: CommandEvent) = if (isRunning) {
        event.reply("so, it looks like the engine is already running").setEphemeral(true).queue()
    } else {
        isRunning = true
        event.reply("great, i'm ready whenever you are").setEphemeral(true).await()
        waitForMessage(event)
    }

    private suspend fun waitForMessage(event: CommandEvent) = event.sandra.shards.await<MessageReceivedEvent> {
        it.author.idLong in Constants.DEVELOPERS && it.message.contentRaw.matches(snippetPattern)
    }.let { process(it, event) }

    private suspend fun process(event: MessageReceivedEvent, commandEvent: CommandEvent) {
        // send a response, so we actually know when the engine is thinking
        val reply = event.message.reply("${Emotes.LOADING} hold on a sec while i crunch the numbers").await()
        // start parsing and building the snippet into a script
        val snippetMatch = snippetPattern.matchEntire(event.message.contentRaw) ?: throw AssertionError("No match")
        val rawSnippet = snippetMatch.groupValues.drop(1).first { it.isNotBlank() }
        // find and rearrange any additional imports. we should also persist these across executions
        val importLines = rawSnippet.lines().takeWhile { it.startsWith("import") }
        persistentImports += importLines.map { it.substringAfter("import ") }
        // now we can actually start building the script
        val variables = buildVariables(event, commandEvent)
        val snippet = rawSnippet.lines().filterNot { it in importLines }.joinToString("\n")
        val allImports = persistentImports.joinToString("\n", postfix = "\n\n") { "import $it" }
        val (value, duration) = execute(allImports + variables + snippet, CoroutineExceptionHandler { _, throwable ->
            handle(reply, "**unknown** with unhandled exception", throwable.stackTraceToString())
        })
        if (value == null) {
            reply.editMessage("evaluated in ${duration.format()} with no returns").await()
        } else handle(reply, duration.format(), value.toString())
        waitForMessage(commandEvent)
    }

    private suspend fun execute(script: String, handler: CoroutineExceptionHandler) = measureTimedValue {
        withContext(engineContext + handler) {
            try {
                scriptEngine.eval(script)
            } catch (throwable: Throwable) {
                throwable
            }
        }
    }

    private fun handle(message: Message, duration: String, result: String) {
        // format the result and make sure it isn't too long to send
        val formatted = "evaluated in $duration\n```\n$result\n```"
        if (formatted.length > Message.MAX_CONTENT_LENGTH) {
            // upload it to hastebin and send the link since it's too big
            hastebin(result)?.let { link ->
                message.editMessage("evaluated in $duration $link").queue()
            } ?: run {
                message.editMessage("upload failed, result was logged").queue()
                logger.info("Evaluation result failed to upload:\n$result")
            }
        } else message.editMessage(formatted).queue()
    }

    private fun buildVariables(event: MessageReceivedEvent, commandEvent: CommandEvent): String {
        val bindings = listOf(
            Triple("channel", event.guildChannel, GuildMessageChannel::class),
            Triple("commands", commandEvent.sandra.commands, CommandManager::class),
            Triple("config", commandEvent.sandra.config, ConfigurationManager::class),
            Triple("gc", commandEvent.sandra.config.getGuild(event.guild.idLong), GuildConfig::class),
            Triple("uc", commandEvent.sandra.config.getUser(event.author.idLong), UserConfig::class),
            Triple("mc", commandEvent.memberConfig, MemberConfig::class),
            Triple("cc", commandEvent.channelConfig, ChannelConfig::class),
            Triple("ce", commandEvent, CommandEvent::class),
            Triple("event", event, MessageReceivedEvent::class),
            Triple("guild", event.guild, Guild::class),
            Triple("id", event.guild.id, String::class),
            Triple("member", event.member, Member::class),
            Triple("redis", commandEvent.sandra.redis, RedisManager::class),
            Triple("sandra", commandEvent.sandra, Sandra::class),
            Triple("shards", commandEvent.sandra.shards, ShardManager::class),
            Triple("user", event.author, User::class),
        )
        // insert the variables into the script engine
        bindings.forEach { scriptEngine.put(it.first, it.second) }
        return bindings.joinToString("\n", postfix = "\n\n") {
            """val ${it.first} = bindings["${it.first}"] as ${it.third.qualifiedName}"""
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Evaluate::class.java)
        private val snippetPattern = Regex("""^```kotlin\n(.+)\n```|^`([^`]+)""", RegexOption.DOT_MATCHES_ALL)
        private val additionalImports = listOf(
            "java.awt.Color",
            "java.io.InputStream",
            "java.time.OffsetDateTime",
            "java.util.*",
            "kotlin.coroutines.*",
            "kotlin.time.Duration",
            "kotlinx.coroutines.*",
            "net.dv8tion.jda.api.*",
            "net.dv8tion.jda.api.entities.*",
            "net.dv8tion.jda.api.interactions.commands.*",
            "net.dv8tion.jda.api.interactions.commands.build.*",
            "net.dv8tion.jda.api.interactions.components.*",
            "net.dv8tion.jda.api.interactions.components.buttons.*",
            "net.dv8tion.jda.api.interactions.components.selections.*",
            "net.dv8tion.jda.api.interactions.components.text.*",
            "net.dv8tion.jda.api.interactions.modals.*",
            "redis.clients.jedis.*"
        )
    }

}
