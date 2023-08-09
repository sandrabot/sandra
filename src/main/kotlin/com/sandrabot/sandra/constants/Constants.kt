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

import com.sandrabot.sandra.BuildInfo

object Constants {

    const val APPLICATION_ID = 302915036492333067L
    const val BETA_APPLICATION_ID = 319951770526941186L

    const val PATREON_CAMPAIGN = 1386347
    const val PATREON_BENEFACTOR = 8834536
    const val PATREON_SPONSOR = 3274217
    const val PATREON_DONOR = 3274216

    const val AVERY = 579335274388258858L
    const val LOGAN = 275012982725935105L
    const val GABBY = 276856260253712384L

    const val GUILD_HANGOUT = 340937384239824897L
    const val GUILD_DEVELOPMENT = 1007480322205679636L

    const val HASTEBIN = "https://haste.averen.dev"
    const val TWITTER_GABBY = "https://twitter.com/CandyCanvasArt"
    const val DIRECT_SUPPORT = "https://discord.gg/sandra"
    const val DIRECT_INVITE = "https://discord.com/api/oauth2/authorize?client_id=$APPLICATION_ID&permissions=473296087&scope=bot%20applications.commands"
    const val BETA_INVITE = "https://discord.com/api/oauth2/authorize?client_id=$BETA_APPLICATION_ID&permissions=274878220352&scope=bot%20applications.commands"

    val DEVELOPERS = arrayOf(AVERY, LOGAN)
    val VERSION = "${BuildInfo.VERSION}_${BuildInfo.COMMIT.take(8)}"
    val USER_AGENT = "Sandra/$VERSION (+${Website.WEBSITE})"

}
