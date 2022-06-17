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

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.config.UserConfig
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.internal.entities.GuildImpl
import net.dv8tion.jda.internal.entities.MemberImpl
import net.dv8tion.jda.internal.entities.UserImpl
import kotlin.reflect.KFunction
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.javaType

private val templateRegex = Regex("""\{([\S.]+)}""")
private val templateTokens = mapOf<String, KFunction<*>>(
    // TODO: Expand and implement more tokens
    "@user" to MemberImpl::getAsMention,
    "user.name" to UserImpl::getName,
    "user.dis" to UserImpl::getDiscriminator,
    "user.id" to MemberImpl::getId,
    "user.tag" to UserImpl::getAsTag,
    "user.level" to UserConfig::level.getter,
    "server.name" to GuildImpl::getName,
    "server.count" to GuildImpl::getMemberCount
)

fun String.formatTemplate(sandra: Sandra, guild: Guild, member: Member): String {
    var formattedText = this
    templateRegex.findAll(this).filter { it.groupValues[1] in templateTokens }.mapNotNull {
        val kFunction = templateTokens[it.groupValues[1]] ?: return@mapNotNull null
        val instance: Any = when (val type = kFunction.instanceParameter?.type?.javaType) {
            GuildImpl::class.javaObjectType -> guild
            MemberImpl::class.javaObjectType -> member
            UserImpl::class.javaObjectType -> member.user
            UserConfig::class.javaObjectType -> sandra.config.getUser(member.idLong)
            else -> throw IllegalArgumentException("Function type is of $type for ${it.value}")
        }
        it.range to when (val result = kFunction.call(instance)) {
            is Number -> "%,d".format(result)
            is String -> result.sanitize()
            else -> throw IllegalArgumentException("Function for ${it.value} returned $result")
        }
    }.toList().asReversed().forEach { (range, value) ->
        formattedText = formattedText.replaceRange(range, value)
    }
    return formattedText
}
