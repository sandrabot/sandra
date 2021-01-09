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

package com.sandrabot.sandra.commands.utility

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.cache.GuildCache
import com.sandrabot.sandra.cache.UserCache
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.entities.SandraGuild
import com.sandrabot.sandra.entities.SandraUser
import com.sandrabot.sandra.events.CommandEvent
import com.sandrabot.sandra.managers.CommandManager
import com.sandrabot.sandra.managers.RedisManager
import com.sandrabot.sandra.utils.await
import com.sandrabot.sandra.utils.duration
import com.sandrabot.sandra.utils.hastebin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Suppress("unused")
class Evaluate : Command(name = "eval", guildOnly = true, ownerOnly = true) {

    private val engineScope = CoroutineScope(EmptyCoroutineContext)
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
        importBuilder.append("""
                import java.awt.Color
                import java.util.*
                import java.util.concurrent.TimeUnit
                import java.time.OffsetDateTime
                import kotlin.coroutines.*
                import kotlinx.coroutines.*
                import net.dv8tion.jda.api.*
                import net.dv8tion.jda.api.entities.*
                import redis.clients.jedis.*
            """.trimIndent()).append("\n\n")
        imports = importBuilder.toString()
    }

    @ExperimentalTime
    override suspend fun execute(event: CommandEvent) {

        if (event.args.isEmpty()) {
            event.reply("i can't evaluate thin air")
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
            Triple("sg", event.sandraGuild, SandraGuild::class),
            Triple("su", event.sandraUser, SandraUser::class),
            Triple("sandra", event.sandra, Sandra::class),
            Triple("shards", event.sandra.shards, ShardManager::class),
            Triple("commands", event.sandra.commands, CommandManager::class),
            Triple("redis", event.sandra.redis, RedisManager::class),
            Triple("guilds", event.sandra.guilds, GuildCache::class),
            Triple("users", event.sandra.users, UserCache::class),
            // Have a way of cancelling running jobs?
            Triple("scope", engineScope, CoroutineScope::class)
        )

        // Insert them into the script engine
        bindings.forEach { engine.put(it.first, it.second) }

        // Prepend the variables to the script so we don't have to use bindings["name"]
        val variables = bindings.joinToString("\n", postfix = "\n\n") {
            """val ${it.first} = bindings["${it.first}"] as ${it.third.qualifiedName}"""
        }

        // Strip the command block if present
        val strippedScript = blockPattern.find(event.args)?.let { it.groupValues[1] } ?: event.args

        // Before we begin the script, we need to inject any additional imports
        val importLines = strippedScript.lines().takeWhile { it.startsWith("import") }
        val additionalImports = importLines.joinToString("\n", postfix = "\n\n")
        val script = strippedScript.lines().filterNot { it in importLines }.joinToString("\n")

        // Evaluate the script *non blocking*, it could take a while to finish
        engineScope.launch {
            // Wait for the message to send before continuing
            val message = event.channel.sendMessage("${Emotes.SPIN} hold on while i crunch the numbers...").await()
            // Measure how long it takes to evaluate the script
            val timedResult = measureTimedValue {
                try {
                    engine.eval(imports + additionalImports + variables + script)
                } catch (exception: Exception) {
                    exception
                }
            }
            val duration = duration(timedResult.duration)
            val result = timedResult.value?.toString() ?: run {
                message.editMessage("evaluated in $duration with no returns").queue()
                return@launch
            }
            // Check the result length, if it's too large attempt to upload it to hastebin
            val formatted = "evaluated in $duration\n```\n$result\n```"
            if (formatted.length > 2000) {
                val hastebin = hastebin(result)
                if (hastebin == null) {
                    message.editMessage("upload failed, printed to stdout").queue()
                    println(result)
                } else message.editMessage(hastebin).queue()
            } else message.editMessage(formatted).queue()
        }

    }

    companion object {
        private val blockPattern = Regex("""```(?:\S*)\n(.*)```""", RegexOption.DOT_MATCHES_ALL)
    }

}
