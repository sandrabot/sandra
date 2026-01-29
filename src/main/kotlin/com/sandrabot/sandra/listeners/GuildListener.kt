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
import com.sandrabot.sandra.constants.ContentStore
import com.sandrabot.sandra.utils.cleanRoleData
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
        LOGGER.info("Added to guild: ${event.guild.name} [${event.guild.id}] | Owned by ${event.guild.ownerId}")
    }

    private fun onGuildLeave(event: GuildLeaveEvent) {
        LOGGER.info("Removed from guild: ${event.guild.name} [${event.guild.id}] | Owned by ${event.guild.ownerId}")
    }

    private fun onMemberJoin(event: GuildMemberJoinEvent) {
        val guildConfig = sandra.config[event.guild]
        val selfMember = event.guild.selfMember

        if (event.user.isBot) {

            // FEATURE: Default Bot Role
            if (guildConfig.autoRolesEnabled && guildConfig.defaultBotRole != 0L) {
                // there's nothing we can do if we don't have this permission
                if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) return
                val botRole = event.guild.getRoleById(guildConfig.defaultBotRole) ?: run {
                    guildConfig.cleanRoleData(event.guild)
                    return
                }
                // verify that we can interact with the bot and their new role
                if (selfMember.canInteract(event.member) && selfMember.canInteract(botRole)) {
                    val reason = ContentStore[event.guild.locale, "core.reasons.bot_role"]
                    event.guild.addRoleToMember(event.member, botRole).reason(reason).queue(null, HANDLER)
                    LOGGER.debug("Default Bot Role: Giving role [{}] to {}", botRole.id, event.member)
                }
            }

            // stop here for bot accounts, nothing else applies to them
            return

        }

        // TODO Feature: Auto Kick

        // TODO Feature: Welcome Messages

        // FEATURE: Auto Roles
        if (guildConfig.autoRolesEnabled) handleAutoRoles(event, guildConfig)
    }

    private fun handleAutoRoles(event: GenericGuildMemberEvent, guildConfig: GuildConfig) {
        val selfMember = event.guild.selfMember

        // there's nothing we can do if we don't have this permission
        if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) return
        // a single role may be given for multiple reasons in certain situations
        val roleMap = mutableMapOf<Role, MutableSet<String>>()
        val memberConfig = guildConfig[event.member]

        // FEATURE: Delayed Default Roles
        if (guildConfig.delayDefaultRoles && event.member.isPending) return

        // FEATURE: Default Roles
        if (guildConfig.defaultRoles.isNotEmpty()) {
            val defaultRoles = guildConfig.defaultRoles.mapNotNull { roleId -> event.guild.getRoleById(roleId) }
            if (guildConfig.defaultRoles.size != defaultRoles.size) guildConfig.cleanRoleData(event.guild)
            defaultRoles.forEach { role -> roleMap.getOrPut(role) { mutableSetOf() }.add("default_roles") }
            LOGGER.debug("Default Roles: Adding entries {} for {}", defaultRoles.map { it.id }, event.member)
        }

        // FEATURE: Saved Roles
        if (guildConfig.saveRolesEnabled && memberConfig.savedRoles.isNotEmpty()) {
            val savedRoles = memberConfig.savedRoles.mapNotNull { roleId -> event.guild.getRoleById(roleId) }
            if (memberConfig.savedRoles.size != savedRoles.size) guildConfig.cleanRoleData(event.guild)
            memberConfig.savedRoles.clear()

            // FEATURE: Revoke Saved Roles
            val allowedRoles = if (guildConfig.revokedRoles.isNotEmpty()) {
                val revokedRoles = guildConfig.revokedRoles.mapNotNull { roleId -> event.guild.getRoleById(roleId) }
                if (guildConfig.revokedRoles.size != revokedRoles.size) guildConfig.cleanRoleData(event.guild)
                LOGGER.debug("Revoked Roles: Removing entries {} for {}", revokedRoles.map { it.id }, event.member)
                savedRoles - revokedRoles.toSet()
            } else savedRoles

            allowedRoles.forEach { role -> roleMap.getOrPut(role) { mutableSetOf() }.add("saved_roles") }
            LOGGER.debug("Saved Roles: Adding entries {} for {}", allowedRoles.map { it.id }, event.member)
        }

        // TODO Feature: Sync Ranks

        // verify that we are able to interact with the given roles
        val reachableMap = roleMap.filterKeys { role ->
            selfMember.canInteract(role)
        }.takeUnless { it.isEmpty() } ?: return

        val distinctReasons = reachableMap.values.asSequence().flatten().distinct().map {
            ContentStore[event.guild.locale, "core.reasons.$it"]
        }.sorted().joinToString()

        val roles = event.member.roles + reachableMap.keys
        event.guild.modifyMemberRoles(event.member, roles).reason(distinctReasons).queue(null, HANDLER)
        LOGGER.debug("Auto Role: Modifying {} roles for {}", reachableMap.size, event.member)
    }

    private fun onMemberRemove(event: GuildMemberRemoveEvent) {
        val guildConfig = sandra.config[event.guild]

        // FEATURE: Save Roles
        if (guildConfig.autoRolesEnabled && guildConfig.saveRolesEnabled) {
            // attempt to store the member's current roles
            // this information is already lost if the member wasn't cached
            // discord does not provide the member meta-data when a remove event is dispatched
            val member = event.member?.takeUnless { it.user.isBot } ?: return
            val roles = member.roles.takeUnless { it.isEmpty() } ?: return
            guildConfig[member].savedRoles += roles.map { it.idLong }
        }
    }

    private fun onMemberUpdatePending(event: GuildMemberUpdatePendingEvent) {
        val guildConfig = sandra.config[event.guild]

        // FEATURE: Delayed Default Roles
        if (guildConfig.autoRolesEnabled && guildConfig.delayDefaultRoles) handleAutoRoles(event, guildConfig)
    }

    private companion object {
        private val HANDLER = ErrorHandler().ignore(ErrorResponse.UNKNOWN_MEMBER, ErrorResponse.MISSING_PERMISSIONS)
        private val LOGGER = LoggerFactory.getLogger(GuildListener::class.java)
    }

}
