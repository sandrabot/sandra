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

import com.sandrabot.sandra.constants.ContentStore
import com.sandrabot.sandra.utils.toLocale
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.DiscordLocale

/**
 * Utility class for conveniently handling translations.
 */
class LocaleContext(val locale: DiscordLocale, val root: String? = null) {

    constructor(guild: Guild?, locale: DiscordLocale, root: String? = null) : this(
        when {
            guild == null -> locale
            locale != guild.locale -> locale
            else -> guild.locale
        }, root
    )

    /**
     * Creates a new context with the specified root.
     */
    fun withRoot(root: String?) = LocaleContext(locale, root)

    /**
     * Returns the unchanged string template from the translation.
     * If [withRoot] is `true`, the context root will be prefixed to the [name].
     *
     * @see get
     * @see getAny
     */
    fun getTemplate(name: String, withRoot: Boolean): String {
        val path = if (root == null || !withRoot) name else "$root.$name"
        return ContentStore[locale, path]
    }

    /**
     * Formats the translation template at [root] + [name] with the [args].
     *
     * @see getAny
     */
    operator fun get(name: String, vararg args: Any?) = getTemplate(name, true).format(locale.toLocale(), *args)

    /**
     * Formats any translation template at [name], without the root.
     *
     * @see get
     */
    fun getAny(name: String, vararg args: Any?) = getTemplate(name, false).format(locale.toLocale(), *args)

}
