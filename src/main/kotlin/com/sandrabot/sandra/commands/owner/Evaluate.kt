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

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.utils.format
import com.sandrabot.sandra.utils.useResourceStream
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.await
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory
import javax.script.Bindings
import javax.script.ScriptEngineManager
import kotlin.time.measureTimedValue

@Suppress("unused")
class Evaluate : Command(guildOnly = true) {

    private val scriptEngine = ScriptEngineManager().getEngineByName("kotlin")
    private val persistentImports = mutableSetOf<String>()
    private val activeChannels = mutableSetOf<Long>()
    private var isEngineStopped = true

    init {
        persistentImports += useResourceStream("imports.txt") { String(readBytes()).lines() }
        persistentImports += Package.getPackages().map { it.name }.filter {
            it.startsWith("com.sandrabot.sandra") && !it.contains("commands")
        }.map { "$it.*" }.sorted()
    }

    override suspend fun execute(event: CommandEvent) {

        if (event.channel.idLong in activeChannels) {
            event.reply("this channel has already enabled eval prompts").setEphemeral(true).queue()
        } else {
            activeChannels += event.channel.idLong
            event.reply("enabled eval prompts for this channel").setEphemeral(true).queue()
            // only allow the engine to be started once, otherwise return
            if (isEngineStopped) isEngineStopped = false else return
            waitForMessage(event.sandra)
        }

    }

    private suspend fun waitForMessage(sandra: Sandra): Unit = sandra.shards.await<MessageReceivedEvent> {
        it.channel.idLong in activeChannels && it.author.idLong in Constants.DEVELOPERS && it.message.contentRaw.matches(snippetRegex)
    }.let {
        handleSnippet(it, sandra)
        waitForMessage(sandra)
    }

    private suspend fun handleSnippet(event: MessageReceivedEvent, sandra: Sandra) {
        // send a response, so we actually know when the engine is thinking
        val reply = event.message.reply("${Emotes.LOADING} hold on a sec while i crunch the numbers").await()
        // start parsing and building the snippet into a script
        val snippetMatch = snippetRegex.matchEntire(event.message.contentRaw) ?: throw AssertionError("No match")
        val rawSnippet = snippetMatch.groupValues.drop(1).first { it.isNotBlank() }
        // find and rearrange any additional imports. we should also persist these across executions
        val importLines = rawSnippet.lines().takeWhile { it.startsWith("import") }
        persistentImports += importLines.map { it.substringAfter("import ") }
        // now we can actually start building the script
        val snippet = rawSnippet.lines().dropWhile { it in importLines || it.isBlank() }.joinToString("\n")
        val allImports = persistentImports.joinToString("\n", postfix = "\n\n") { "import $it" }
        // create a handler to catch any unexpected exceptions
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            handleResult(reply, "**unknown** with unhandled exception", throwable.stackTraceToString())
        }
        // finally create bindings and evaluate the script
        val (result, duration) = evaluate(allImports + snippet, createBindings(event, sandra), exceptionHandler)
        if (result == null) {
            reply.editMessage("evaluated in ${duration.format()} with no returns").await()
        } else handleResult(reply, duration.format(), result.toString())
        val debugImports = if (logger.isDebugEnabled) allImports else ""
        // log the context, script, and result of the evaluation
        logger.info("""
            |Evaluated snippet submitted by ${event.author.name} [${event.author.idLong}] in ${event.channel.name} [${event.channel.idLong}]
            |$debugImports$snippet
            |
            |======================================== RESULT ($duration) ========================================
            |
            |$result
        """.trimMargin())
    }

    private suspend fun evaluate(
        script: String, bindings: Bindings, handler: CoroutineExceptionHandler
    ) = measureTimedValue {
        withContext(handler) {
            try {
                scriptEngine.eval(script, bindings)
            } catch (throwable: Throwable) {
                throwable
            }
        }
    }

    private fun handleResult(message: Message, duration: String, result: String) {
        // format the result and make sure it isn't too long to send
        val content = "evaluated in $duration\n```\n$result\n```"
        if (content.length > Message.MAX_CONTENT_LENGTH) {
            // otherwise just rely on the console output for the result
            message.editMessage("evaluated in $duration, check console for result").queue()
        } else message.editMessage(content).queue()
    }

    private fun createBindings(messageEvent: MessageReceivedEvent, sandra: Sandra) =
        scriptEngine.createBindings().apply {
            val guildConfig = sandra.config.getGuild(messageEvent.guild.idLong)
            put("event", messageEvent)
            put("guild", messageEvent.guild)
            put("id", messageEvent.guild.id)
            put("channel", messageEvent.guildChannel)
            put("member", messageEvent.member)
            put("user", messageEvent.author)
            put("gc", guildConfig)
            put("cc", guildConfig.getChannel(messageEvent.channel.idLong))
            put("mc", guildConfig.getMember(messageEvent.author.idLong))
            put("uc", sandra.config.getUser(messageEvent.author.idLong))
            put("sandra", sandra)
            put("commands", sandra.commands)
            put("config", sandra.config)
            put("redis", sandra.redis)
            put("shards", sandra.shards)
        }

    private companion object {
        private val logger = LoggerFactory.getLogger(Evaluate::class.java)
        private val snippetRegex = Regex("""^```kotlin\n(.+)\n```|^`([^`]+)""", RegexOption.DOT_MATCHES_ALL)
    }

}
