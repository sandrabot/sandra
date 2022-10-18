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
import com.sandrabot.sandra.constants.Colors
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.listeners.InteractionListener
import com.sandrabot.sandra.listeners.MessageListener
import com.sandrabot.sandra.listeners.ReadyListener
import com.sandrabot.sandra.managers.*
import com.sandrabot.sandra.services.BotListService
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.CoroutineEventManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.messages.MessageRequest
import org.slf4j.LoggerFactory
import java.util.*

/**
 * This class is the heart and soul of the bot. To avoid static abuse,
 * it must be used to access other systems throughout the codebase.
 */
class Sandra(val sandraConfig: SandraConfig, val redis: RedisManager, val credentials: CredentialManager) {

    val development = sandraConfig.development
    val color = if (development) Colors.WELL_READ else Colors.SEA_SERPENT
    val shards: ShardManager

    val api = RequestManager(this, sandraConfig.apiPort)
    val blocklist = BlocklistManager(this)
    val botList = BotListService(this)
    val config = ConfigurationManager(this)
    val lang = TranslationManager()
    val commands = CommandManager(this)
    val messages = MessageManager()
    val patreon = PatreonManager(this)
    val statistics = StatisticsManager()

    init {

        // Eliminate the possibility of accidental mass mentions, if a command needs @role it can be overridden
        val disabledMentioned = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE)
        MessageRequest.setDefaultMentions(EnumSet.complementOf(disabledMentioned))
        MessageRequest.setDefaultMentionRepliedUser(false)

        // Configure JDA settings, we've got a couple of them
        logger.info("Configuring JDA and signing into Discord")
        val token = if (development) credentials.betaToken else credentials.token
        val builder = DefaultShardManagerBuilder.createDefault(token)
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
        builder.setMemberCachePolicy(MemberCachePolicy.ALL)
        builder.setChunkingFilter(ChunkingFilter.include(Constants.GUILD_HANGOUT))
        builder.setShardsTotal(sandraConfig.shardsTotal)
        builder.setStatus(OnlineStatus.IDLE)
        builder.setEventManagerProvider { CoroutineEventManager() }
        builder.setBulkDeleteSplittingEnabled(false)
        builder.setEnableShutdownHook(false)

        // Register our listeners so we actually receive the events
        builder.addEventListeners(
            // The ready listener will remove itself after startup finishes
            InteractionListener(this), MessageListener(this), ReadyListener(this)
        )

        // Block the thread until the first shard signs in
        shards = builder.build()

        val self = shards.shardCache.first().selfUser
        logger.info("Signed into Discord as ${self.asTag} (${self.id})")

        if (sandraConfig.apiEnabled) api.start()

    }

    /**
     * Factory method to create embed templates.
     */
    fun createEmbed() = EmbedBuilder().setColor(color)

    suspend fun retrieveUser(userId: Long): User? = shards.retrieveUserById(userId).await()
    suspend fun retrieveUser(userId: String): User? = shards.retrieveUserById(userId).await()

    /**
     * Gracefully closes all resources and shuts down the bot.
     * Any other shutdown hooks registered in the JVM will not run.
     *
     * Setting [restart] to `true` will cause the application to exit with
     * error code 2, which is to be interpreted by a process control system.
     */
    fun shutdown(restart: Boolean = false) {
        // Figure out who started the shutdown and log the caller
        val caller = try {
            // Element 0 is getStackTrace, 1 is this method, 2 might be shutdown$default or the caller
            val stackTrace = Thread.currentThread().stackTrace
            val indexTwo = stackTrace[2]
            // Checking the method name is probably good enough
            val caller = if (indexTwo.methodName == "shutdown\$default") stackTrace[3] else indexTwo
            "${caller.className}::${caller.methodName}"
        } catch (ignored: Exception) {
            null
        }
        logger.info("Shutdown called by $caller with restart: $restart")

        if (sandraConfig.apiEnabled) api.shutdown()
        shards.shutdown()
        blocklist.shutdown()
        config.shutdown()
        redis.shutdown()

        val code = if (restart) 2 else 0
        logger.info("Finished shutting down, halting runtime with code $code")
        Runtime.getRuntime().halt(code)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Sandra::class.java)
    }

}
