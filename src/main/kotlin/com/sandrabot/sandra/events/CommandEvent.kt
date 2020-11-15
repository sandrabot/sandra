/*
 * Copyright 2017-2020 Avery Clifton and Logan Devecka
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
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.entities.*
import com.sandrabot.sandra.utils.commandPath
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

    val commandPath: String = commandPath(command)
    val isOwner: Boolean = author.idLong in Constants.OWNERS
    val cooldownKey: String = when (command.cooldownScope) {
        CooldownScope.USER -> "U:${author.id}|${commandPath}"
        CooldownScope.CHANNEL -> "C:${channel.id}|${commandPath}"
        CooldownScope.GUILD -> "G:${guild.id}|${commandPath}"
        CooldownScope.SHARD -> "S:${jda.shardInfo.shardId}|${commandPath}"
        CooldownScope.COMMAND -> "C:${commandPath}"
    }

    val arguments: ArgumentResult by lazy { Argument.parse(this, command.arguments) }
    val languageContext: LanguageContext by lazy { LanguageContext(sandra, sandraGuild, sandraUser) }
    val sandraGuild: SandraGuild by lazy { sandra.guilds.get(guild.idLong) }
    val sandraUser: SandraUser by lazy { sandra.users.get(author.idLong) }

    fun translate(path: String, vararg args: Any?): String {
        return languageContext.translate(path, *args)
    }

    fun reply(message: String, success: ((Message) -> Unit)? = null, failure: ((Throwable) -> Unit)? = null) {
        event.channel.sendMessage(message).queue(success, failure)
    }

    fun reply(embed: MessageEmbed, success: ((Message) -> Unit)? = null, failure: ((Throwable) -> Unit)? = null) {
        event.channel.sendMessage(embed).queue(success, failure)
    }

    fun replyError(message: String, success: ((Message) -> Unit)? = null, failure: ((Throwable) -> Unit)? = null) {
        reply("${Emotes.ERROR} $message", success, failure)
    }

}
