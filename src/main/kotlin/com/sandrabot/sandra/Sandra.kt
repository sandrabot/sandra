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

package com.sandrabot.sandra

import com.sandrabot.sandra.api.SandraAPI
import com.sandrabot.sandra.config.SandraConfig
import com.sandrabot.sandra.constants.Colors
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.listeners.CommandListener
import com.sandrabot.sandra.listeners.EventWaiter
import com.sandrabot.sandra.listeners.ReadyListener
import com.sandrabot.sandra.managers.*
import com.sandrabot.sandra.services.BotListService
import com.sandrabot.sandra.services.CooldownService
import com.sandrabot.sandra.services.PresenceService
import com.sandrabot.sandra.utils.await
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.slf4j.LoggerFactory
import java.util.*

/**
 * This is the main class for the bot.
 */
class Sandra(sandraConfig: SandraConfig, val redis: RedisManager, val credentials: CredentialManager) {

    val apiEnabled = sandraConfig.apiEnabled
    val development = sandraConfig.development
    val color = if (development) Colors.RED else Colors.BLURPLE
    val prefix = if (development) Constants.BETA_PREFIX else Constants.PREFIX

    val api = SandraAPI(this, sandraConfig.apiPort)
    val blocklist = BlocklistManager(this)
    val botList = BotListService(this)
    val config = ConfigurationManager(this)
    val commands = CommandManager(this)
    val cooldowns = CooldownManager(this)
    val cooldownService = CooldownService(this)
    val eventManager = EventManager()
    val eventWaiter = EventWaiter()
    val languages = LanguageManager()
    val patreon = PatreonManager(this)
    val presence = PresenceService(this)
    val statistics = StatisticsManager()

    val shards: ShardManager

    private val logger = LoggerFactory.getLogger(Sandra::class.java)

    init {

        // Configure the development presence
        if (development) presence.setDevelopment()

        // Eliminate the possibility of accidental mass mentions, if a command needs @role it can be overridden
        val disabledMentioned = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE)
        MessageAction.setDefaultMentions(EnumSet.complementOf(disabledMentioned))

        // Configure JDA settings, we've got a couple of them
        logger.info("Configuring JDA and signing into Discord")
        val token = if (development) credentials.betaToken else credentials.token
        val builder = DefaultShardManagerBuilder.createDefault(token)
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS)
        builder.setMemberCachePolicy(MemberCachePolicy.ALL)
        builder.setChunkingFilter(ChunkingFilter.include(Constants.GUILD_HANGOUT))
        builder.setShardsTotal(sandraConfig.shardsTotal)
        builder.setStatus(OnlineStatus.IDLE)
        builder.addEventListeners(eventManager)
        builder.setBulkDeleteSplittingEnabled(false)
        builder.setEnableShutdownHook(false)

        // Register our event listeners
        eventManager.register(CommandListener(this), ReadyListener(this), eventWaiter)

        // Block the thread until the first shard signs in
        shards = builder.build()

        val self = shards.shardCache.first().selfUser
        logger.info("Signed into Discord as ${self.asTag} (${self.id})")

        commands.setMentionPrefixes(self.id)
        if (apiEnabled) api.start()

    }

    /**
     * Returns a new embed with the current color.
     * By using a centralized factory, templates can be tweaked.
     */
    fun createEmbed() = EmbedBuilder().setColor(color)

    /**
     * For use in coroutines to asynchronously retrieve users.
     */
    suspend fun retrieveUser(userId: Long): User? = shards.retrieveUserById(userId).await()

    /**
     * **Use with caution.** Blocks the thread while retrieving a user.
     * This should only be used when you cannot access a coroutine scope.
     */
    fun completeUser(userId: String): User? = completeUser(userId.toLong())

    /**
     * **Use with caution.** Blocks the thread while retrieving a user.
     * This should only be used when you cannot access a coroutine scope.
     */
    fun completeUser(userId: Long): User? = try {
        shards.retrieveUserById(userId).complete()
    } catch (e: Exception) {
        null
    }

    /**
     * Gracefully closes all resources and shuts down the bot.
     * Any other shutdown hooks registered in the JVM will not run.
     *
     * Setting [restart] to true will exit with code 2 which is to
     * be interpreted by a control system to restart the process.
     */
    fun shutdown(restart: Boolean = false) {
        // Figure out who called this method and log the caller
        val caller = try {
            // Element 0 is getStackTrace(), 1 is this method, 2 might be shutdown$default or the caller
            val stackTrace = Thread.currentThread().stackTrace
            val indexTwo = stackTrace[2]
            // Checking the method name is probably good enough
            val caller = if (indexTwo.methodName == "shutdown\$default") {
                stackTrace[3]
            } else indexTwo
            "${caller.className}::${caller.methodName}"
        } catch (ignored: Exception) {
            null
        }
        logger.info("Shutdown called by $caller with restart: $restart")

        if (apiEnabled) api.shutdown()
        shards.shutdown()
        blocklist.shutdown()
        cooldowns.shutdown()
        config.shutdown()
        redis.shutdown()

        val code = if (restart) 2 else 0
        logger.info("Finished shutting down, halting with code $code")
        Runtime.getRuntime().halt(code)
    }

}
