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
import com.sandrabot.sandra.listeners.InteractionListener
import com.sandrabot.sandra.listeners.LoggingListener
import com.sandrabot.sandra.listeners.MessageListener
import com.sandrabot.sandra.listeners.ReadyListener
import com.sandrabot.sandra.managers.*
import com.sandrabot.sandra.services.BotListService
import dev.minn.jda.ktx.events.CoroutineEventManager
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager

/**
 * This class is the heart and soul of the bot. It provides
 * access to Sandra's internal services throughout the application.
 */
class Sandra(val settings: SandraConfig, val redis: RedisManager) {

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

    val shards: ShardManager = DefaultShardManagerBuilder.createDefault(settings.secrets.token).apply {
        enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
        setEventManagerProvider { CoroutineEventManager() }
        setShardsTotal(settings.shardsTotal)
        setStatus(OnlineStatus.IDLE)
        setBulkDeleteSplittingEnabled(false)
        setEnableShutdownHook(false)
        addEventListeners(
            LoggingListener(this@Sandra),
            InteractionListener(this@Sandra),
            MessageListener(this@Sandra),
            ReadyListener(this@Sandra)
        )
    }.build()

}
