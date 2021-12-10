/*
 * Copyright 2017-2021 Avery Carroll and Logan Devecka
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
import com.sandrabot.sandra.config.GuildConfig
import com.sandrabot.sandra.config.UserConfig
import com.sandrabot.sandra.utils.findLocale

/**
 * Wrapper class for conveniently retrieving translation keys.
 */
class LocaleContext(
    private val sandra: Sandra,
    val locale: Locale,
    val root: String? = null
) {

    constructor(sandra: Sandra, guildConfig: GuildConfig?, userConfig: UserConfig, root: String? = null) :
            this(sandra, findLocale(guildConfig, userConfig), root)

    /**
     * Returns a new context with the new root.
     */
    fun withRoot(root: String?) = LocaleContext(sandra, locale, root)

    /**
     * Returns the raw translation. The lookup path will
     * include the root when [withRoot] is `true`.
     */
    fun get(path: String, withRoot: Boolean) = sandra.locales.get(
        locale, if (root == null || !withRoot) path else "$root.$path"
    )

    /**
     * Formats the translation with the [args] provided.
     */
    fun translate(path: String, vararg args: Any?) = translate(path, true, *args)

    /**
     * Formats the translation with the [args] provided.
     * If [withRoot] is `true` the context root will be used for lookup.
     */
    fun translate(path: String, withRoot: Boolean, vararg args: Any?) = get(path, withRoot).format(*args)

}
