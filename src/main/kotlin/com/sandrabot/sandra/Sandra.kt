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

package com.sandrabot.sandra

import com.sandrabot.sandra.api.SandraAPI
import com.sandrabot.sandra.cache.GuildCache
import com.sandrabot.sandra.cache.UserCache
import com.sandrabot.sandra.config.SandraConfig
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.CountingThreadFactory
import com.sandrabot.sandra.listeners.ReadyListener
import com.sandrabot.sandra.managers.CredentialManager
import com.sandrabot.sandra.managers.EventManager
import com.sandrabot.sandra.managers.RedisManager
import com.sandrabot.sandra.managers.StatisticsManager
import com.sandrabot.sandra.services.BotListService
import com.sandrabot.sandra.services.PresenceService
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * This is the main class for the bot.
 */
class Sandra(sandraConfig: SandraConfig, val redis: RedisManager, val credentials: CredentialManager) {

    val apiEnabled = sandraConfig.apiEnabled
    val developmentMode = sandraConfig.developmentMode
    val prefix = if (developmentMode) Constants.BETA_PREFIX else Constants.PREFIX
    val cacheExecutor: ExecutorService = Executors.newCachedThreadPool(CountingThreadFactory("cache"))

    val botList = BotListService(this)
    val eventManager = EventManager()
    val guilds = GuildCache(this)
    val presence = PresenceService(this)
    val sandraApi = SandraAPI(this, sandraConfig.apiPort)
    val statistics = StatisticsManager()
    val users = UserCache(this)

    val shards: ShardManager

    private val logger = LoggerFactory.getLogger(Sandra::class.java)

    init {

        // Configure the development presence
        if (developmentMode) presence.setDevelopment()

        // Configure JDA settings, we've got a lot of them
        val token = if (developmentMode) credentials.betaToken else credentials.token
        val disabledIntents = EnumSet.of(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_TYPING)
        val builder = DefaultShardManagerBuilder.create(token, EnumSet.complementOf(disabledIntents))
        builder.disableCache(EnumSet.of(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS))
        builder.setMemberCachePolicy(MemberCachePolicy.DEFAULT)
        builder.setChunkingFilter(ChunkingFilter.NONE)
        builder.setStatus(OnlineStatus.IDLE)
        builder.setShardsTotal(sandraConfig.shardsTotal)
        builder.setBulkDeleteSplittingEnabled(false)
        builder.setRelativeRateLimit(developmentMode)
        builder.setEnableShutdownHook(false)
        builder.setEventManagerProvider { eventManager }

        // Register the event listeners
        eventManager.registerAll(ReadyListener(this))

        logger.info("Building JDA and signing into Discord")
        // Blocks the thread until the first shard signs in
        shards = builder.build()

        val self = shards.shardCache.first().selfUser
        logger.info("Signed into Discord as ${self.asTag} (${self.id})")

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

        if (apiEnabled) sandraApi.shutdown()
        shards.shutdown()
        cacheExecutor.shutdown()
        // Prevent data loss by waiting for pending operations
        cacheExecutor.awaitTermination(2, TimeUnit.SECONDS)
        redis.shutdown()

        val code = if (restart) 2 else 0
        logger.info("Finished shutting down, halting with code $code")
        Runtime.getRuntime().halt(code)
    }

}
