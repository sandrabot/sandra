/*
 * Copyright 2017-2021 Avery Carroll and Logan Devecka
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
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege
import org.slf4j.LoggerFactory

class ReadyListener(private val sandra: Sandra) {

    // We need this to track the shards that have
    // finished, shards may disconnect during startup
    private var shardsReady = 0

    @Suppress("unused")
    fun onReady(event: ReadyEvent) {

        shardsReady++
        // Update the shard's presence, it is currently set to idle
        sandra.presence.update(event.jda)
        val shardInfo = event.jda.shardInfo
        // The last shard to finish loading initializes most of the bot
        if (shardsReady == shardInfo.shardTotal) {
            if (!sandra.development) {
                sandra.presence.start()
                sandra.botList.start()
            }

            val logger = LoggerFactory.getLogger(ReadyListener::class.java)
            // Update the global slash command list, so that they always match the commands we have
            val (owner, global) = sandra.commands.values.partition { it.ownerOnly }
            event.jda.updateCommands().addCommands(global.mapNotNull { it.asCommandData(sandra) }).queue { commands ->
                commands.forEach { sandra.commands[it.name]?.id = it.idLong}
                logger.info("Successfully replaced global command list with ${commands.size} commands")
            }
            // Update the owner slash commands in Sandra's Hangout as well as their privileges
            sandra.shards.getGuildById(Constants.GUILD_HANGOUT)?.let { hangout ->
                hangout.updateCommands().addCommands(owner.mapNotNull { it.asCommandData(sandra) }).queue { commands ->
                    commands.forEach { sandra.commands[it.name]?.id = it.idLong}
                    val privileges = Constants.DEVELOPERS.map { CommandPrivilege.enableUser(it) }
                    hangout.updateCommandPrivileges(commands.associate { it.id to privileges }).queue()
                    logger.info("Successfully replaced owner command list with ${commands.size} commands")
                }
            }

            logger.info("Shard ${shardInfo.shardId} has finished starting additional items")
            // This is the last ready event that will fire, so we don't need this listener anymore
            sandra.eventManager.unregister(this)
        }

    }

}
