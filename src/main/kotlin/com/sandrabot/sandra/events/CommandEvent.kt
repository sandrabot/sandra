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
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class CommandEvent(
    val sandra: Sandra,
    val event: MessageReceivedEvent,
    val command: Command,
    val args: String
) {

    val author: User
        get() = event.author
    val channel: MessageChannel
        get() = event.channel
    val guild: Guild
        get() = event.guild
    val isFromGuild: Boolean
        get() = event.isFromGuild
    val jda: JDA
        get() = event.jda
    val member: Member
        get() = if (event.isFromGuild) event.member!! else
            throw IllegalStateException("This message did not happen in a guild")
    val message: Message
        get() = event.message
    val selfMember: Member
        get() = guild.selfMember
    val selfUser: SelfUser
        get() = jda.selfUser
    val textChannel: TextChannel
        get() = event.textChannel

    val embed: EmbedBuilder
        get() = sandra.createEmbed()

    val commandPath: String = command.path
    val isOwner: Boolean = author.idLong in Constants.DEVELOPERS

    val arguments: ArgumentResult by lazy { parseArguments(command.arguments, this, args) }
    val guildConfig: GuildConfig by lazy { sandra.config.getGuild(guild.idLong) }
    val userConfig: UserConfig by lazy { sandra.config.getUser(author.idLong) }
    val patreonTier: PatreonTier? by lazy { sandra.patreon.getUserTier(author.idLong) }
    val localeContext: LocaleContext by lazy {
        LocaleContext(sandra, guildConfig, userConfig, "commands.${command.name}")
    }

    fun translate(path: String, vararg args: Any?): String = translate(path, true, *args)
    fun translate(path: String, withRoot: Boolean, vararg args: Any?): String =
        localeContext.translate(path, withRoot, *args)

    fun reply(message: String, success: ((Message) -> Unit)? = null, failure: ((Throwable) -> Unit)? = null) {
        event.message.reply(message).queue(success, failure)
    }

    fun reply(embed: MessageEmbed, success: ((Message) -> Unit)? = null, failure: ((Throwable) -> Unit)? = null) {
        event.message.replyEmbeds(embed).queue(success, failure)
    }

    fun reply(message: Message, success: ((Message) -> Unit)? = null, failure: ((Throwable) -> Unit)? = null) {
        event.message.reply(message).queue(success, failure)
    }

    fun reply(any: Any, success: ((Message) -> Unit)? = null, failure: ((Throwable) -> Unit)? = null) {
        reply(any.toString(), success, failure)
    }

    fun replyEmote(
        message: String, emote: String, success: ((Message) -> Unit)? = null, failure: ((Throwable) -> Unit)? = null
    ) = reply(emote + Unicode.VERTICAL_LINE + message, success, failure)

    fun replyInfo(message: String, success: ((Message) -> Unit)? = null, failure: ((Throwable) -> Unit)? = null) {
        replyEmote(message, Emotes.INFO, success, failure)
    }

    fun replyError(message: String, success: ((Message) -> Unit)? = null, failure: ((Throwable) -> Unit)? = null) {
        replyEmote(message, Emotes.ERROR, success, failure)
    }

}
