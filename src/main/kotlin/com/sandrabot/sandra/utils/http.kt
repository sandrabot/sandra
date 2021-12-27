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

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.constants.Constants
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

val httpClient = HttpClient(OkHttp) {
    install(JsonFeature)
    defaultRequest {
        header("User-Agent", Constants.USER_AGENT)
    }
}

inline fun <reified T> getBlocking(
    url: String, crossinline block: HttpRequestBuilder.() -> Unit = {}
): T = runBlocking(Dispatchers.IO) {
    httpClient.get(url) {
        apply(block)
    }
}

inline fun <reified T> postBlocking(
    url: String, content: Any, crossinline block: HttpRequestBuilder.() -> Unit = {}
): T = runBlocking(Dispatchers.IO) {
    httpClient.post(url) {
        body = content
        apply(block)
    }
}
