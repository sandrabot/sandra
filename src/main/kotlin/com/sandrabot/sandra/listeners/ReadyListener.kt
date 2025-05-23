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

package com.sandrabot.sandra.listeners

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.CoroutineEventListener
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.slf4j.LoggerFactory

class ReadyListener(private val sandra: Sandra) : CoroutineEventListener {

    // keep a total number of shards that have finished loading
    private var shardsReady = 0

    override suspend fun onEvent(event: GenericEvent) {
        if (event is ReadyEvent) onReady(event)
    }

    private suspend fun onReady(event: ReadyEvent) {
        shardsReady += 1
        // change the status from idle to online, this signifies the shard is ready
        val status = if (sandra.settings.development) OnlineStatus.DO_NOT_DISTURB else OnlineStatus.ONLINE
        event.jda.presence.setPresence(status, Activity.customStatus("/help — sandrabot.com [${event.jda.shardInfo.shardId}]"))
        val shardInfo = event.jda.shardInfo
        // only the last shard to load will initialize the rest of our services
        if (shardsReady == shardInfo.shardTotal) {
            sandra.subscriptions.start()
            if (sandra.settings.apiEnabled) sandra.api?.start()
            if (!sandra.settings.development) sandra.botList.start()
            // if command updates are enabled, now is the time to perform the updates
            if (sandra.settings.commandUpdates) try {
                // update the global slash command list, this makes sure the commands match our local commands
                val topCommands = sandra.commands.values.filterNot { it.isSubcommand }
                val (owner, global) = topCommands.partition { it.isOwnerOnly }.toList().map { list ->
                    list.map { command -> sandra.commands.commandData[command.path] }
                }
                val globalCommands = event.jda.updateCommands().addCommands(global).await()
                logger.info("Successfully updated global command list with ${globalCommands.size} commands")
                // update the owner command list for all of our development servers
                arrayOf(
                    Constants.GUILD_HANGOUT, Constants.GUILD_DEVELOPMENT
                ).mapNotNull { sandra.shards.getGuildById(it) }.forEach { guild ->
                    val ownerCommands = guild.updateCommands().addCommands(owner).await()
                    logger.info("Successfully updated owner command list with ${ownerCommands.size} commands for ${guild.id}")
                }
            } catch (t: Throwable) {
                logger.error("An exception occurred while updating command lists", t)
            } else logger.warn("Slash command updates have been disabled, changes will not be reflected")
            logger.info("Shard ${shardInfo.shardString} has completed all additional tasks, ready to serve ${sandra.shards.guildCache.size()} guilds")
            // this is the last ready event we will ever care about, so we don't need this listener anymore
            sandra.shards.removeEventListener(this)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ReadyListener::class.java)
    }

}
