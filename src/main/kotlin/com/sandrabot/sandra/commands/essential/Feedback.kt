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

package com.sandrabot.sandra.commands.essential

import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.constants.Emotes
import com.sandrabot.sandra.constants.Unicode
import com.sandrabot.sandra.entities.Command
import com.sandrabot.sandra.events.CommandEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button

@Suppress("unused")
class Feedback : Command(name = "feedback") {

    override suspend fun execute(event: CommandEvent) {

        val button = Button.link(Constants.DIRECT_SUPPORT, event.translate("button_label"))
        val reply = Emotes.INFO + Unicode.VERTICAL_LINE + event.translate("reply")
        event.reply(reply).addActionRow(button).setEphemeral(true).queue()

    }

}
