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

package com.sandrabot.sandra.entities

import com.sandrabot.sandra.Sandra

class LanguageContext(
        private val sandra: Sandra,
        sandraGuild: SandraGuild,
        sandraUser: SandraUser,
        var root: String? = null
) {

    // TODO Use the preferences to decide the locale
    private val decidedLocale: Locale = Locale.ENGLISH

    fun get(path: String) = sandra.languages.get(decidedLocale, if (root == null) path else "$root.$path")
    fun getFormatted(path: String, vararg args: Any?) = get(path).format(*args)

}