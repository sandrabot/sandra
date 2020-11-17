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

package com.sandrabot.sandra.managers

/**
 * This class keeps track of sensitive tokens and secrets used throughout the bot.
 */
data class CredentialManager(

        /* ========== Discord Tokens ========== */

        /**
         * The token for the production bot account.
         */
        val token: String,

        /**
         * The token for the development bot account.
         */
        val betaToken: String,

        /* ======= Miscellaneous Secrets ======= */

        /**
         * The creator's access token for your patreon client.
         */
        val patreonToken: String,

        /* ========== Bot List Tokens ========== */

        /**
         * The token for using the API at https://botlist.space
         */
        val spaceToken: String,

        /**
         * The token for using the API at https://bots.ondiscord.xyz
         */
        val bodToken: String,

        /**
         * The token for using the API at https://discord.boats
         */
        val boatToken: String,

        /**
         * The token for using the API at https://discordbotlist.com
         */
        val dblToken: String,

        /**
         * The token for using the API at https://discord.bots.gg
         */
        val dbgToken: String,

        /**
         * The token for using the api at https://top.gg
         */
        val topGgToken: String

)
