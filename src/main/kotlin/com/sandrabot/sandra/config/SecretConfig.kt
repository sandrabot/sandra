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

package com.sandrabot.sandra.config

import kotlinx.serialization.Serializable

/**
 * Object that holds secrets and tokens used throughout the bot.
 */
@Serializable
data class SecretConfig(

    /**
     * Token used for the production bot account.
     */
    val productionToken: String = "",

    /**
     * Token used for the development bot account.
     */
    val developmentToken: String = "",

    /**
     * Token used for the API at https://botlist.space
     */
    val spaceToken: String = "",

    /**
     * Token used for the API at https://bots.ondiscord.xyz
     */
    val bodToken: String = "",

    /**
     * Token used for the API at https://discord.boats
     */
    val boatToken: String = "",

    /**
     * Token used for the API at https://discordbotlist.com
     */
    val dblToken: String = "",

    /**
     * Token used for the API at https://discord.bots.gg
     */
    val dbgToken: String = "",

    /**
     * Token used for the api at https://top.gg
     */
    val topGgToken: String = "",

    )
