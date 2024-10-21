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

@file:OptIn(ExperimentalSerializationApi::class)

package com.sandrabot.sandra.constants

import com.sandrabot.sandra.exceptions.MissingTranslationException
import com.sandrabot.sandra.utils.flatten
import com.sandrabot.sandra.utils.useResourceStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import net.dv8tion.jda.api.interactions.DiscordLocale

/**
 * This class is used to load and store all the content strings for the bot.
 */
object ContentStore {

    private val contentMap: Map<DiscordLocale, Map<String, Any>>

    /**
     * Set of locales that are available for use.
     */
    val locales: Set<DiscordLocale>

    init {
        val dataMap = mutableMapOf<DiscordLocale, MutableMap<String, Any>>()
        for (locale in AvailableContent.entries) {
            // parse the content file by streaming it and flattening the paths
            val content = useResourceStream("content/${locale.commonName}.json") {
                Json.decodeFromStream<JsonObject>(this).flatten()
            }
            // use the metadata to reliably select the appropriate locale
            val discordLocale = DiscordLocale.from(content["meta.locale"] as String)
            // prevent metadata from being entered into the content map
            dataMap.getOrPut(discordLocale) { mutableMapOf() }.putAll(content.filterKeys { !it.startsWith("meta") })
        }
        // prevent runtime modifications to the content map
        contentMap = dataMap.toMap()
        locales = contentMap.keys
    }

    /**
     * Get the content string for the specified locale and name. If the locale
     * is unavailable, the default locale will be used instead (en-US).
     * If the entry contains multiple values, a random one will be chosen.
     *
     * @throws MissingTranslationException if the content string is unavailable
     *
     * @see getList
     */
    operator fun get(locale: DiscordLocale, name: String): String = when (val content = getAny(locale, name)) {
        is String -> content
        is List<*> -> content.random() as String
        else -> throw AssertionError("Illegal type for translation path $locale/$name = $content")
    }

    /**
     * Allows you to retrieve the entire list of possible values for a content string.
     *
     * @see get
     */
    fun getList(locale: DiscordLocale, name: String): List<String> = getAny(locale, name).let {
        if (it is List<*>) it.filterIsInstance<String>() else {
            throw IllegalArgumentException("Translation path $locale/$name is not a list")
        }
    }

    private fun getAny(discordLocale: DiscordLocale, name: String): Any {
        val locale = if (discordLocale in contentMap) discordLocale else DiscordLocale.ENGLISH_US
        val translationMap = contentMap[locale] ?: throw AssertionError("Missing default translation map")
        return translationMap[name] ?: throw MissingTranslationException(locale, name)
    }

}
