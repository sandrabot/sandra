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

package com.sandrabot.sandra

import com.sandrabot.sandra.api.RequestManager
import com.sandrabot.sandra.config.SandraConfig
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.listeners.InteractionListener
import com.sandrabot.sandra.listeners.MessageListener
import com.sandrabot.sandra.listeners.ReadyListener
import com.sandrabot.sandra.managers.*
import com.sandrabot.sandra.services.BotListService
import dev.minn.jda.ktx.events.CoroutineEventManager
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.messages.MessageRequest
import org.slf4j.LoggerFactory
import java.util.*

/**
 * This class is the heart and soul of the bot. It provides
 * global access to all internal services throughout the bot.
 */
class Sandra(val sandraConfig: SandraConfig, val redis: RedisManager, val credentials: CredentialManager) {

    val development = sandraConfig.development
    val shards: ShardManager

    // initialization order is important here, commands depends on translations
    val api = RequestManager(this, sandraConfig.apiPort)
    val blocklist = BlocklistManager(this)
    val botList = BotListService(this)
    val config = ConfigurationManager(this)
    val lang = TranslationManager()
    val commands = CommandManager(this)
    val messages = MessageManager()
    val subscriptions = SubscriptionManager(this)
    val statistics = StatisticsManager()

    init {

        // eliminate the possibility of accidental mass mentions, if commands need to @role it can be overridden
        val disabled = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE)
        MessageRequest.setDefaultMentions(EnumSet.complementOf(disabled))

        // create the shard manager and configure all additional settings
        logger.info("Configuring shard manager and signing into Discord...")
        val token = if (development) credentials.betaToken else credentials.token
        val builder = DefaultShardManagerBuilder.createDefault(token)
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
        builder.setChunkingFilter(ChunkingFilter.include(Constants.GUILD_HANGOUT, Constants.GUILD_DEVELOPMENT))
        builder.setMemberCachePolicy(MemberCachePolicy.DEFAULT)
        builder.setShardsTotal(sandraConfig.shardsTotal)
        builder.setStatus(OnlineStatus.IDLE)
        builder.setEventManagerProvider { CoroutineEventManager() }
        builder.setBulkDeleteSplittingEnabled(false)
        builder.setEnableShutdownHook(false)

        // register all event listeners for the shard manager
        // the ready listener will remove itself after startup is finished
        builder.addEventListeners(InteractionListener(this), MessageListener(this), ReadyListener(this))

        // build the shard manager and wait for it to sign in to Discord
        // this will block the main thread until the bot is ready to go
        shards = builder.build()

        logger.info("Sandra has signed into Discord under the account: ${shards.shards[0].selfUser.asTag}")
        if (sandraConfig.apiEnabled) api.start()

    }

    /**
     * Gracefully closes all resources and shuts down the bot.
     * Any other shutdown hooks registered in the JVM will not run.
     *
     * When [restart] is `true`, the application will exit with error code 2,
     * this is to be interpreted by a process control system as a restart.
     */
    fun shutdown(restart: Boolean = false) {
        logger.info("Shutdown requested, closing all resources...")

        // we want to know how the shutdown was triggered, so we can log it
        val trigger = try {
            // elements 0 and 1 are the current thread and this method, 2 might be the caller
            val stack = Thread.currentThread().stackTrace
            val caller = if (stack[2].methodName == "shutdown\$default") stack[3] else stack[2]
            "${caller.className}.${caller.methodName}(${caller.fileName}:${caller.lineNumber})"
        } catch (t: Throwable) {
            "unknown"
        }
        logger.info("Shutdown was triggered by: $trigger (restart: $restart)")

        // close all resources and shutdown the shard manager
        // order is important here, we want to close the database last
        api.shutdown()
        shards.shutdown()
        blocklist.shutdown()
        config.shutdown()
        redis.shutdown()

        val code = if (restart) 2 else 0
        logger.info("Shutdown complete, exiting with code $code")
        // halt the JVM, this will prevent any other shutdown hooks from running
        Runtime.getRuntime().halt(code)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Sandra::class.java)
    }

}
