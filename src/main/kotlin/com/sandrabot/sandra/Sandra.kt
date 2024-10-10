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

package com.sandrabot.sandra

import com.sandrabot.sandra.api.ServerController
import com.sandrabot.sandra.config.SandraConfig
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.listeners.InteractionListener
import com.sandrabot.sandra.listeners.MessageListener
import com.sandrabot.sandra.listeners.ReadyListener
import com.sandrabot.sandra.managers.*
import com.sandrabot.sandra.services.BotListService
import dev.minn.jda.ktx.events.CoroutineEventManager
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter

/**
 * This class is the heart and soul of the bot. It provides
 * global access to all internal services throughout the bot.
 */
class Sandra(val settings: SandraConfig, val redis: RedisManager) {

    val shards: ShardManager

    // only initialize the api when enabled, this preserves resources and reduces logging
    val api = if (settings.apiEnabled) ServerController(this) else null

    val commands = CommandManager()
    val messages = MessageManager()
    val statistics = StatisticsManager()
    val blocklist = BlocklistManager(this)
    val botList = BotListService(this)
    val config = ConfigurationManager(this)
    val lastfm = LastRequestManager(this)
    val subscriptions = SubscriptionManager(this)

    init {
        val token = if (settings.development) settings.secrets.developmentToken else settings.secrets.productionToken
        val builder = DefaultShardManagerBuilder.createDefault(token)
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
        builder.setChunkingFilter(ChunkingFilter.include(Constants.GUILD_HANGOUT, Constants.GUILD_DEVELOPMENT))
        builder.setEventManagerProvider { CoroutineEventManager() }
        builder.setShardsTotal(settings.shardsTotal)
        builder.setStatus(OnlineStatus.IDLE)
        builder.setBulkDeleteSplittingEnabled(false)
        builder.setEnableShutdownHook(false)

        // register all event listeners for the shard manager
        builder.addEventListeners(InteractionListener(this), MessageListener(this), ReadyListener(this))

        shards = builder.build()
    }

}
