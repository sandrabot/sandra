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

package com.sandrabot.sandra.entities

import com.sandrabot.sandra.events.CommandEvent
import net.dv8tion.jda.api.Permission
import kotlin.reflect.full.createInstance

abstract class Command(
        val name: String,
        val aliases: Array<String> = emptyArray(),
        arguments: String = "",
        val guildOnly: Boolean = false,
        val ownerOnly: Boolean = false,
        val cooldown: Int = 2000,
        val cooldownScope: CooldownScope = CooldownScope.USER,
        val botPermissions: Array<Permission> = emptyArray(),
        val userPermissions: Array<Permission> = emptyArray()
) {

    val arguments = Argument.compile(arguments)
    val category = Category.fromClass(this::class)
    val children = this::class.nestedClasses.mapNotNull {
        if (it is Command) it.createInstance() as Command else null
    }.toList()

    abstract suspend fun execute(event: CommandEvent)

}
