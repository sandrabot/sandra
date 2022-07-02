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

package com.sandrabot.sandra.listeners

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.CoroutineEventListener
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import org.slf4j.LoggerFactory

class ReadyListener(private val sandra: Sandra) : CoroutineEventListener {

    // keep a total number of shards that have finished loading
    private var shardsReady = 0

    override suspend fun onEvent(event: GenericEvent) {
        if (event is ReadyEvent) onReady(event)
    }

    private suspend fun onReady(event: ReadyEvent) {
        shardsReady += 1
        // change the status from idle to something else to signify the shard is ready
        event.jda.presence.setStatus(if (sandra.development) OnlineStatus.DO_NOT_DISTURB else OnlineStatus.ONLINE)
        val shardInfo = event.jda.shardInfo
        // only the last shard to load will initialize the rest of our services
        if (shardsReady == shardInfo.shardTotal) {
            if (!sandra.development) sandra.botList.start()
            // if command updates are enabled, now is the time to perform the updates
            if (sandra.sandraConfig.commandUpdates) {
                // update the global slash command list, this makes sure the commands match our local commands
                val (owner, global) = sandra.commands.values.partition { it.ownerOnly }
                val commands = event.jda.updateCommands().addCommands(global.mapNotNull { it.asCommandData(sandra) }).await()
                commands.forEach { sandra.commands[it.name]?.id = it.idLong }
                logger.info("Successfully updated global command list with ${commands.size} commands")
                // update the command list for all of our development servers
                arrayOf(Constants.GUILD_HANGOUT, Constants.GUILD_DEVELOPMENT).mapNotNull {
                    sandra.shards.getGuildById(it)
                }.forEachIndexed { index, guild ->
                    val await = guild.updateCommands().addCommands(owner.mapNotNull { it.asCommandData(sandra) }).await()
                    if (index == 0) await.forEach { sandra.commands[it.name]?.id = it.idLong }
                    logger.info("Successfully updated owner commands with ${await.size} commands for ${guild.id}")
                }
            } else logger.warn("Slash command updates have been disabled, changes will not be reflected")
            logger.info("Shard ${shardInfo.shardString} has finished loading additional items")
            // this is the last ready event we will ever care about, so we don't need this listener anymore
            sandra.eventManager.unregister(this)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ReadyListener::class.java)
    }

}
