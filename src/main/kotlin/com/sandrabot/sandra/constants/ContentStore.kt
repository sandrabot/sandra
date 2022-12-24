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

@file:OptIn(ExperimentalSerializationApi::class)

package com.sandrabot.sandra.constants

import com.sandrabot.sandra.exceptions.MissingTranslationException
import com.sandrabot.sandra.utils.flatten
import com.sandrabot.sandra.utils.resourceAsStream
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
        val tempMap = mutableMapOf<DiscordLocale, MutableMap<String, Any>>()
        // read the content directory to find available resources
        for (entry in readDirectory("content")) {
            // decode content from json file and flatten into a path map
            val content = resourceAsStream(entry) { Json.decodeFromStream<JsonObject>(this).flatten() }
            // read the metadata to reliably select the appropriate locale
            val locale = DiscordLocale.from(content["meta.locale"] as String)
            // ensure that entries are not overwritten, prevent metadata from being entered
            tempMap.getOrPut(locale) { mutableMapOf() }.putAll(content.filterKeys { !it.startsWith("meta") })
        }
        contentMap = tempMap
        locales = tempMap.keys
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

    private fun getAny(locale: DiscordLocale, name: String): Any {
        val localeMap = contentMap[if (locale in contentMap) locale else DiscordLocale.ENGLISH_US]
            ?: throw MissingTranslationException("Missing default translation map")
        return localeMap[name] ?: throw MissingTranslationException("Missing content for $locale at: $name")
    }

    private fun readDirectory(name: String) =
        this.javaClass.classLoader.getResourceAsStream(name)?.reader()?.useLines { sequence ->
            sequence.filterNot { it.isBlank() }.map { "$name/$it" }.toList()
        } ?: throw AssertionError("Failed to load: $name")

}
