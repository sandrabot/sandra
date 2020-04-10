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

package com.sandrabot.sandra.listeners

import com.sandrabot.sandra.Sandra
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener

class ReadyListener(private val sandra: Sandra) : EventListener {

    // We need this to track the shards that have
    // finished, shards may disconnect during startup
    private var shardsReady = 0

    override fun onEvent(event: GenericEvent) {

        if (event is ReadyEvent) {
            // A shard finished loading
            shardsReady++
            // Update the shard's presence, it is currently "booting"
            sandra.presence.update(event.jda)
            // The last shard to finish loading initializes most of the bot
            if (shardsReady == event.jda.shardInfo.shardTotal) {
                if (sandra.apiEnabled) sandra.sandraApi.start()
                if (!sandra.developmentMode) {
                    sandra.presence.start()
                    sandra.botList.start()
                }
                // This is the last ready event that will ever
                // fire, so we don't need this listener anymore
                sandra.shards.removeEventListener(this)
            }
        }

    }

}
