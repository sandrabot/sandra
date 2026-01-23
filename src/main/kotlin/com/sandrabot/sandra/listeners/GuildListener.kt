/*
 * Copyright 2017-2026 Avery Carroll and Logan Devecka
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
import com.sandrabot.sandra.config.GuildConfig
import dev.minn.jda.ktx.events.CoroutineEventListener
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import org.slf4j.LoggerFactory

/**
 * Event listener that processes join and leave events for guilds and their members.
 */
class GuildListener(private val sandra: Sandra) : CoroutineEventListener {

    override suspend fun onEvent(event: GenericEvent) {
        when (event) {
            is GuildJoinEvent -> onGuildJoin(event)
            is GuildLeaveEvent -> onGuildLeave(event)

            is GuildMemberJoinEvent -> onMemberJoin(event)
            is GuildMemberRemoveEvent -> onMemberRemove(event)
            is GuildMemberUpdatePendingEvent -> onMemberUpdatePending(event)
        }
    }

    private fun onGuildJoin(event: GuildJoinEvent) {
        LOGGER.info("Joined guild: ${event.guild.name} [${event.guild.id}] | Owned by ${event.guild.ownerId}")
    }

    private fun onGuildLeave(event: GuildLeaveEvent) {
        LOGGER.info("Left guild: ${event.guild.name} [${event.guild.id}] | Owned by ${event.guild.ownerId}")
    }

    private fun onMemberJoin(event: GuildMemberJoinEvent) {
        val guildConfig = sandra.config[event.guild]
        val selfMember = event.guild.selfMember

        // FEATURE: Default Bot Role
        if (event.user.isBot) {
            if (guildConfig.autoRolesEnabled && guildConfig.defaultBotRole != 0L) {
                if (!selfMember.hasPermission(Permission.MANAGE_ROLES) || !selfMember.canInteract(event.member)) return
                val defaultBotRole = event.guild.getRoleById(guildConfig.defaultBotRole) ?: run {
                    // TODO role data is stale, schedule data cleanup
                    return
                }
                if (selfMember.canInteract(defaultBotRole)) {
                    // TODO localised reasons for auditable actions
                    event.guild.addRoleToMember(event.member, defaultBotRole).reason("default bot role").queue()
                    LOGGER.debug("Default Bot Role: Giving {} to {}", defaultBotRole, event.member)
                }
            }
            // stop here for bot accounts, nothing else applies to them
            return
        }

        // TODO Feature: Auto Kick

        // TODO Feature: Welcome Messages

        // FEATURE: Auto Roles
        handleAutoRoles(event, guildConfig)
    }

    private fun handleAutoRoles(event: GenericGuildMemberEvent, guildConfig: GuildConfig) {
        if (!guildConfig.autoRolesEnabled || !event.guild.selfMember.hasPermission(Permission.MANAGE_ROLES)) return
        // a single role may be given for different reasons in certain situations
        val roleMap = mutableMapOf<Role, MutableSet<String>>()
        val memberConfig = guildConfig[event.member]

        // FEATURE: Delayed Default Roles
        if (guildConfig.delayDefaultRoles && event.member.isPending) {
            if (guildConfig.saveRolesEnabled && memberConfig.savedRoles.isNotEmpty()) {
                // continue if roles should be restored, bypass member verification for known members
            } else return
        }

        // FEATURE: Default Roles
        val defaultRoles = guildConfig.defaultRoles.mapNotNull { roleId -> event.guild.getRoleById(roleId) }
        if (guildConfig.defaultRoles.size != defaultRoles.size) {
            // TODO role data is stale, schedule data cleanup
        }
        // TODO localised reasons for auditable actions
        defaultRoles.forEach { role -> roleMap.getOrPut(role) { mutableSetOf() }.add("default roles") }
        LOGGER.debug("Auto Role: Adding {} default roles for {}: {}", defaultRoles.size, event.member, defaultRoles)

        // FEATURE: Save Roles
        if (guildConfig.saveRolesEnabled && memberConfig.savedRoles.isNotEmpty()) {
            val savedRoles = memberConfig.savedRoles.mapNotNull { roleId -> event.guild.getRoleById(roleId) }
            if (memberConfig.savedRoles.size != savedRoles.size) {
                // TODO role data is stale, schedule data cleanup
            }
            memberConfig.savedRoles.clear()
            // TODO localised reasons for auditable actions
            savedRoles.forEach { role -> roleMap.getOrPut(role) { mutableSetOf() }.add("previously saved roles") }
            LOGGER.debug("Auto Role: Adding {} saved roles for {}: {}", savedRoles.size, event.member, savedRoles)
        }

        // TODO Feature: Sync Ranks

        // TODO optionally block certain roles from being reapplied automatically (mods, admins)

        // ensure that we are able to interact with the given roles
        val reachableMap = roleMap.filterKeys { role -> event.guild.selfMember.canInteract(role) }
        // do nothing if none of the roles are within reach
        if (reachableMap.isEmpty()) return

        val reason = reachableMap.flatMap { (_, reasons) -> reasons }.toSet().sorted().joinToString()
        LOGGER.debug("Auto Role: Modifying {} roles for {} [{}]", reachableMap.size, event.member, reachableMap.keys)
        event.guild.modifyMemberRoles(event.member, reachableMap.keys).reason(reason).queue(null, HANDLER)
    }

    private fun onMemberRemove(event: GuildMemberRemoveEvent) {
        val guildConfig = sandra.config[event.guild]

        // FEATURE: Save Roles
        if (guildConfig.autoRolesEnabled && guildConfig.saveRolesEnabled) {
            // attempt to store the member's current roles, this information is already lost if the member wasn't cached
            val member = event.member ?: return
            guildConfig[member].savedRoles += member.roles.map { it.idLong }
        }
    }

    private fun onMemberUpdatePending(event: GuildMemberUpdatePendingEvent) {
        // FEATURE: Delayed Default Roles
        handleAutoRoles(event, sandra.config[event.guild])
    }

    private companion object {
        private val HANDLER = ErrorHandler().ignore(ErrorResponse.UNKNOWN_MEMBER, ErrorResponse.MISSING_PERMISSIONS)
        private val LOGGER = LoggerFactory.getLogger(GuildListener::class.java)
    }

}
