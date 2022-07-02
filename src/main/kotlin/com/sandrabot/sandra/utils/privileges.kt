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

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.entities.Privilege
import com.sandrabot.sandra.events.CommandEvent
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

fun GuildConfig.isExperienceAllowed(event: MessageReceivedEvent) = experiencePrivileges.isAllowed(event)
fun GuildConfig.isExperienceAllowed(event: CommandEvent) = experiencePrivileges.isAllowed(event)

fun List<Privilege>.isAllowed(event: CommandEvent): Boolean = isAllowed(event.guildChannel, event.member)
fun List<Privilege>.isAllowed(event: MessageReceivedEvent): Boolean = isAllowed(event.guildChannel, event.member)

// TODO: Privilege permission checks
fun List<Privilege>.isAllowed(channel: GuildChannel, member: Member?): Boolean = true