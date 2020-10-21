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

package com.sandrabot.sandra.services

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.entities.Service

/**
 * Removes expired entries from the cooldown manager.
 */
class CooldownService(private val sandra: Sandra) : Service(60) {

    override fun execute() = sandra.cooldowns.clean()

}
