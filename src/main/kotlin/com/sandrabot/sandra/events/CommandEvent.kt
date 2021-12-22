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

package com.sandrabot.sandra.events

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionHook

class CommandEvent(
    val sandra: Sandra, val event: SlashCommandEvent, val command: Command
) {

    val jda: JDA get() = event.jda
    val isFromGuild: Boolean get() = event.isFromGuild
    val isAcknowledged: Boolean get() = event.isAcknowledged
    val interaction: Interaction get() = event.interaction
    val hook: InteractionHook get() = event.hook

    val textChannel: TextChannel get() = event.textChannel
    val channel: MessageChannel get() = event.channel
    val embed: EmbedBuilder get() = sandra.createEmbed()

    val guild: Guild? = event.guild
    val member: Member? = event.member
    val selfMember: Member? = guild?.selfMember
    val selfUser: SelfUser get() = jda.selfUser
    val user: User get() = event.user

    val commandPath: String = command.path
    val commandString: String = event.commandString
    val isOwner: Boolean = user.idLong in Constants.DEVELOPERS

    val arguments: ArgumentResult by lazy { parseArguments(command.arguments, this, "") }
    val guildConfig: GuildConfig? by lazy { guild?.let { sandra.config.getGuild(it.idLong) } }
    val userConfig: UserConfig by lazy { sandra.config.getUser(user.idLong) }
    val patreonTier: PatreonTier? by lazy { sandra.patreon.getUserTier(user.idLong) }
    val localeContext: LocaleContext by lazy {
        LocaleContext(sandra, guildConfig, userConfig, "commands.${command.name}")
    }

    fun translate(path: String, vararg args: Any?): String = translate(path, true, *args)
    fun translate(path: String, withRoot: Boolean, vararg args: Any?): String =
        localeContext.translate(path, withRoot, *args)

    fun deferReply(ephemeral: Boolean = false) = event.deferReply(ephemeral)

    fun reply(message: String) = event.reply(message)
    fun reply(message: Message) = event.reply(message)
    fun reply(embed: MessageEmbed) = event.replyEmbeds(embed)
    fun reply(vararg embeds: MessageEmbed) = event.replyEmbeds(embeds.asList())

    fun sendMessage(message: String) = event.hook.sendMessage(message)
    fun sendMessage(message: Message) = event.hook.sendMessage(message)
    fun sendMessage(embed: MessageEmbed) = event.hook.sendMessageEmbeds(embed)
    fun sendMessage(vararg embeds: MessageEmbed) = event.hook.sendMessageEmbeds(embeds.asList())

    fun replyEmote(message: String, emote: String) = reply(emote + Unicode.VERTICAL_LINE + message)
    fun replyInfo(message: String) = replyEmote(message, Emotes.INFO)
    fun replyError(message: String) = replyEmote(message, Emotes.ERROR)

}
