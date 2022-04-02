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

package com.sandrabot.sandra.entities

import kotlinx.serialization.Serializable

@Serializable
class Privilege(val type: Type, val id: Long, val allowed: Boolean) {

    override fun toString(): String {
        return "Privilege:$type:$id($allowed)"
    }

    enum class Type {
        CATEGORY, CHANNEL, ROLE, USER
    }

}
