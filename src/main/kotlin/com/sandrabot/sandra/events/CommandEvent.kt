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

package com.sandrabot.sandra.events

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.config.ChannelConfig
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.MemberConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.Colors
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.*
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.util.*

class CommandEvent(
    val sandra: Sandra, val event: SlashCommandInteractionEvent, val command: Command
) {

    val jda: JDA get() = event.jda
    val isFromGuild: Boolean get() = event.isFromGuild
    val isAcknowledged: Boolean get() = event.isAcknowledged
    val interaction: Interaction get() = event.interaction
    val options: List<OptionMapping> get() = event.options
    val hook: InteractionHook get() = event.hook

    val guildChannel: GuildMessageChannel get() = event.guildChannel
    val channel: MessageChannel get() = event.channel
    val embed: EmbedBuilder get() = EmbedBuilder().setColor(Colors.WELL_READ)

    val guild: Guild? = event.guild
    val member: Member? = event.member
    val selfMember: Member? = guild?.selfMember
    val selfUser: SelfUser get() = jda.selfUser
    val user: User get() = event.user

    val commandPath: String = command.path
    val commandString: String = event.commandString
    val encodedInteraction: String = Base64.getEncoder()
        .encodeToString("${user.id}:${channel.id}:${interaction.id}".encodeToByteArray())
    val argumentString: String = commandString.substringAfter(" ", missingDelimiterValue = "")
    val isOwner: Boolean = user.idLong in Constants.DEVELOPERS

    val arguments: ArgumentResult by lazy { parseArguments(this, command.arguments) }
    val guildConfig: GuildConfig? by lazy { guild?.let { sandra.config.getGuild(it.idLong) } }
    val channelConfig: ChannelConfig? by lazy { guildConfig?.getChannel(guildChannel.idLong) }
    val memberConfig: MemberConfig? by lazy { guildConfig?.getMember(user.idLong) }
    val userConfig: UserConfig by lazy { sandra.config.getUser(user.idLong) }
    val subscriptions: Set<Subscription> by lazy { sandra.subscriptions[user.idLong] }
    val localeContext: LocaleContext by lazy {
        LocaleContext(sandra, guild, interaction.userLocale, "commands.${command.name}")
    }

    suspend fun retrieveUser(id: Long): User? = sandra.shards.retrieveUserById(id).await()

    fun get(path: String, vararg args: Any?): String = localeContext.get(path, *args)
    fun getAny(path: String, vararg args: Any?): String = localeContext.getAny(path, *args)

    fun deferReply(ephemeral: Boolean = false) = event.deferReply(ephemeral)

    fun reply(message: String) = event.reply(message)
    fun reply(data: MessageCreateData) = event.reply(data)
    fun reply(embed: MessageEmbed) = event.replyEmbeds(embed)
    fun reply(embeds: List<MessageEmbed>) = event.replyEmbeds(embeds)

    fun sendMessage(message: String) = event.hook.sendMessage(message)
    fun sendMessage(data: MessageCreateData) = event.hook.sendMessage(data)
    fun sendMessage(embed: MessageEmbed) = event.hook.sendMessageEmbeds(embed)
    fun sendMessage(embeds: List<MessageEmbed>) = event.hook.sendMessageEmbeds(embeds)

    fun sendEmote(message: String, emote: String) = sendMessage("$emote $message")
    fun sendInfo(message: String) = sendEmote(message, Emotes.INFO)
    fun sendError(message: String) = sendEmote(message, Emotes.FAILURE)

    fun replyEmote(message: String, emote: String) = reply("$emote $message")
    fun replyInfo(message: String) = replyEmote(message, Emotes.INFO)
    fun replyError(message: String) = replyEmote(message, Emotes.FAILURE)

}
