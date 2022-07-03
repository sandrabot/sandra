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

import com.sandrabot.sandra.Sandra
import net.dv8tion.jda.api.entities.Guild
import java.util.*

/**
 * Utility class for conveniently handling translations.
 */
class LocaleContext(private val sandra: Sandra, val locale: Locale, val root: String? = null) {

    constructor(sandra: Sandra, guild: Guild?, locale: Locale, root: String? = null) : this(
        sandra, when {
            guild == null -> locale
            locale != guild.locale -> locale
            else -> guild.locale
        }, root
    )

    /**
     * Creates a new context with the specified root.
     */
    fun withRoot(root: String?) = LocaleContext(sandra, locale, root)

    /**
     * Returns the unchanged string template from the translation.
     * If [withRoot] is `true`, the context root will be prefixed to the [path].
     *
     * @see get
     * @see getAny
     */
    fun getTemplate(path: String, withRoot: Boolean): String = sandra.lang.get(
        locale, if (root == null || !withRoot) path else "$root.$path"
    )

    /**
     * Formats the translation template at [root] + [path] with the [args].
     *
     * @see getAny
     */
    fun get(path: String, vararg args: Any?) = getTemplate(path, true).format(locale, *args)

    /**
     * Formats any translation template at [path], without the root.
     *
     * @see get
     */
    fun getAny(path: String, vararg args: Any?) = getTemplate(path, false).format(locale, *args)

}
