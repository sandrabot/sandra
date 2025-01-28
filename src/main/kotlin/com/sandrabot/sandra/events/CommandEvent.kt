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

package com.sandrabot.sandra.events

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.config.ChannelConfig
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.MemberConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessagePollData

/**
 * Provides all the necessary context for command execution.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CommandEvent(
    val command: Command, val event: SlashCommandInteractionEvent, val sandra: Sandra,
) {

    val jda: JDA = event.jda
    val isFromGuild: Boolean = event.isFromGuild
    val isAcknowledged: Boolean get() = event.isAcknowledged
    val interaction: SlashCommandInteraction = event.interaction
    val commandString: String = event.commandString
    val options: List<OptionMapping> = event.options
    val hook: InteractionHook = event.hook
    val id: Long = event.idLong

    val user: User = event.user
    val guild: Guild? = event.guild
    val member: Member? = event.member
    val selfMember: Member? = guild?.selfMember
    val selfUser: SelfUser = event.jda.selfUser
    val channel: MessageChannelUnion = event.channel

    val userConfig: UserConfig by lazy { sandra.config[user] }
    val guildConfig: GuildConfig? by lazy { guild?.let { sandra.config[it] } }
    val channelConfig: ChannelConfig? by lazy { guildConfig?.getChannel(channel.idLong) }
    val memberConfig: MemberConfig? by lazy { guildConfig?.getMember(user.idLong) }
    val isOwner: Boolean = user.idLong in Constants.DEVELOPERS

    val arguments: ArgumentResult by lazy { parseArguments(this, command.arguments) }
    val subscriptions: Set<Subscription> by lazy { sandra.subscriptions[user.idLong] }
    val localeContext = LocaleContext(guild, event.userLocale, "commands.${command.path}")

    fun get(path: String, vararg args: Any?): String = localeContext.get(path, *args)
    fun getAny(path: String, vararg args: Any?): String = localeContext.getAny(path, *args)

    fun embed(): EmbedBuilder = EmbedBuilder().setColor(sandra.settings.color)

    fun deferReply(ephemeral: Boolean = false) = event.deferReply(ephemeral)

    fun reply(content: String) = event.reply(content)
    fun reply(message: MessageCreateData) = event.reply(message)
    fun replyEmbeds(embed: MessageEmbed) = event.replyEmbeds(embed)
    fun replyEmbeds(embeds: Collection<MessageEmbed>) = event.replyEmbeds(embeds)
    fun replyComponents(component: LayoutComponent) = event.replyComponents(component)
    fun replyComponents(components: Collection<LayoutComponent>) = event.replyComponents(components)
    fun replyModal(modal: Modal) = event.replyModal(modal)
    fun replyPoll(poll: MessagePollData) = event.replyPoll(poll)

    fun replyEmoji(emoji: String, content: String) = reply("$emoji $content")
    fun replyEmoji(emoji: Emoji, content: String) = replyEmoji(emoji.formatted, content)
    fun replyInfo(content: String) = replyEmoji(Emotes.INFO, content)
    fun replySuccess(content: String) = replyEmoji(Emotes.SUCCESS, content)
    fun replyWarning(content: String) = replyEmoji(Emotes.NOTICE, content)
    fun replyError(content: String) = replyEmoji(Emotes.FAILURE, content)

    fun sendMessage(content: String) = event.hook.sendMessage(content)
    fun sendMessage(message: MessageCreateData) = event.hook.sendMessage(message)
    fun sendMessageEmbeds(embed: MessageEmbed) = event.hook.sendMessageEmbeds(embed)
    fun sendMessageEmbeds(embeds: Collection<MessageEmbed>) = event.hook.sendMessageEmbeds(embeds)
    fun sendMessageComponents(component: LayoutComponent) = event.hook.sendMessageComponents(component)
    fun sendMessageComponents(components: Collection<LayoutComponent>) = event.hook.sendMessageComponents(components)
    fun sendMessagePoll(poll: MessagePollData) = event.hook.sendMessagePoll(poll)

    fun sendEmoji(emoji: String, content: String) = sendMessage("$emoji $content")
    fun sendEmoji(emoji: Emoji, content: String) = sendEmoji(emoji.formatted, content)
    fun sendInfo(content: String) = sendEmoji(Emotes.INFO, content)
    fun sendSuccess(content: String) = sendEmoji(Emotes.SUCCESS, content)
    fun sendWarning(content: String) = sendEmoji(Emotes.NOTICE, content)
    fun sendError(content: String) = sendEmoji(Emotes.FAILURE, content)

}

fun ReplyCallbackAction.asEphemeral() = setEphemeral(true)
fun <T> WebhookMessageCreateAction<T>.asEphemeral() = setEphemeral(true)
