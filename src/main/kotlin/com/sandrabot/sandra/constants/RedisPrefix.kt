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

package com.sandrabot.sandra.constants

/**
 * These prefixes are used to categorize and to perform
 * bulk operations on keys in the redis database.
 */
enum class RedisPrefix(private val prefix: String) {

    GUILD("G:"), USER("U:"), SETTING("S:");

    override fun toString() = prefix

    operator fun plus(other: String) = prefix + other

}
