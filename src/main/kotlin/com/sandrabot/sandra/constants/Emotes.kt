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

package com.sandrabot.sandra.constants

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.EmojiUnion

fun String.asEmoji(): EmojiUnion = Emoji.fromFormatted(this)

@Suppress("unused")
object Emotes {

    const val ADD = "<:add:1278458839812800694>"
    const val ARROW_LEFT = "<:arrow_left:1278461505507168368>"
    const val ARROW_RIGHT = "<:arrow_right:1278461529171165194>"
    const val BAN = "<:ban:1278461546481062016>"
    const val BIN = "<:bin:1278461561727619193>"
    const val CASH = "<:cash:1278463932050313239>"
    const val CHAT = "<:chat:1278461582443155558>"
    const val COMMANDS = "<:commands:1278461605763350549>"
    const val CONFIG = "<:config:1278461621601177650>"
    const val DISABLED = "<:disabled:1278461638860738622>"
    const val DOWNVOTE = "<:downvote:1279317659498057850>"
    const val ENABLED = "<:enabled:1278461653901643920>"
    const val FAILURE = "<:failure:1278461668728504380>"
    const val FAST_FORWARD = "<:fast_forward:1278461684117409792>"
    const val FOLDER = "<:folder:1278461701611847712>"
    const val FUN = "<:fun:1278461721349984268>"
    const val HOME = "<:home:1278461740698435584>"
    const val INFO = "<:info:1278461761858834613>"
    const val INVITE = "<:invite:1278461780347195404>"
    const val JOIN = "<:join:1278461800312213637>"
    const val LASTFM = "<:lastfm:1278548049605558364>"
    const val LEAVE = "<:leave:1278461819354353726>"
    const val LEVEL_UP = "<:level_up:1278461840329936938>"
    const val LOADING = "<a:loading:1278461861792186437>"
    const val MOD = "<:mod:1278461890129039471>"
    const val MUSIC = "<:music:1278461910269956116>"
    const val NOTICE = "<:notice:1278461934890389546>"
    const val NUMBER = "<:number:1278461961138343998>"
    const val PATREON = "<:patreon:1278464600186028116>"
    const val PIN = "<:pin:1278461988825075855>"
    const val PLAY_PAUSE = "<:play_pause:1278462011440894023>"
    const val PROMPT = "<:prompt:1278462036715503670>"
    const val RESET = "<:reset:1278462059633315890>"
    const val RETURN = "<:return:1278462080395247687>"
    const val REWIND = "<:rewind:1278462100598951947>"
    const val SUBTRACT = "<:subtract:1278462122745004102>"
    const val SUCCESS = "<:success:1278462142999429152>"
    const val TAILS = "<:tails:1278464893380722688>"
    const val TIME = "<:time:1278462162381312155>"
    const val UPVOTE = "<:upvote:1279317593173266505>"
    const val USER = "<:user:1278462178684440597>"
    const val VOLUME_DOWN = "<:volume_down:1278462195738349568>"
    const val VOLUME_UP = "<:volume_up:1278462213337645098>"

}
