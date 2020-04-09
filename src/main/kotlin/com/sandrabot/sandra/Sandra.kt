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

import com.sandrabot.sandra.config.SandraConfig
import com.sandrabot.sandra.managers.CredentialManager
import com.sandrabot.sandra.managers.RedisManager
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.LoggerFactory
import java.util.*

class Sandra(sandraConfig: SandraConfig, val redis: RedisManager, val credentials: CredentialManager) {

    val developmentMode = sandraConfig.developmentMode

    val shards: ShardManager

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        // Initialize JDA and the event listeners
        val token = if (developmentMode) credentials.betaToken else credentials.token
        val disabledIntents = EnumSet.of(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_TYPING)
        val builder = DefaultShardManagerBuilder.create(token, EnumSet.complementOf(disabledIntents))
        builder.disableCache(EnumSet.of(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS))
        builder.setMemberCachePolicy(MemberCachePolicy.DEFAULT)
        builder.setShardsTotal(sandraConfig.totalShards)
        builder.setChunkingFilter(ChunkingFilter.NONE)
        builder.setBulkDeleteSplittingEnabled(false)
        builder.setRelativeRateLimit(developmentMode)
        builder.setEnableShutdownHook(false)
        logger.info("Building JDA and signing into Discord...")
        shards = builder.build()
    }

}