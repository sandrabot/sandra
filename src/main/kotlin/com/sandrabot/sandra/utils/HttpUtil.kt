/*
 *    Copyright 2017-2020 Avery Clifton and Logan Devecka
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.sandrabot.sandra.utils

import com.sandrabot.sandra.constants.Constants
import okhttp3.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * A collection of methods to perform HTTP requests using OkHttp,
 * organizing and preventing duplicate code throughout the bot.
 */
class HttpUtil {

    companion object {

        /**
         * Used to represent a request body as json content.
         */
        val APPLICATION_JSON: MediaType = MediaType.get("application/json; charset=utf-8")

        private val logger = LoggerFactory.getLogger(HttpUtil::class.java)
        private val client: OkHttpClient

        init {
            val builder = OkHttpClient.Builder()
            builder.addInterceptor {
                val requestBuilder = it.request().newBuilder()
                requestBuilder.header("User-Agent", Constants.USER_AGENT)
                it.proceed(requestBuilder.build())
            }
            client = builder.build()
        }

        /**
         * Creates a generic body using the [content] provided.
         */
        fun createBody(content: String?) = createBody(null, content)

        /**
         * Creates a request body to be used with [createRequest].
         */
        fun createBody(mediaType: MediaType?, content: String?): RequestBody {
            return RequestBody.create(mediaType, content ?: "")
        }

        /**
         * Creates a generic request using the [url] provided.
         */
        fun createRequest(url: String) = createRequest(url, null, null)

        /**
         * Creates a request to be used with [execute].
         */
        fun createRequest(url: String, method: String?, body: RequestBody?): Request.Builder {
            val builder = Request.Builder().url(url)
            if (method != null && body != null) {
                builder.method(method, body)
            }
            return builder
        }

        /**
         * Downloads a remote resource to the provided [destination].
         */
        fun download(url: String, destination: File): Boolean {
            val input = getStream(url)
            val output = FileOutputStream(destination)
            return try {
                val positiveBytes = input.transferTo(output) > 0
                input.close(); output.close()
                positiveBytes
            } catch (e: Exception) {
                logger.error("Failed to download resource from url $url")
                false
            }
        }

        /**
         * Executes a request, returning the response as a string.
         */
        fun execute(request: Request): String {
            val body = handle(request)?.body()
            return try {
                body?.string() ?: ""
            } catch (e: Exception) {
                logger.error("Failed to load request response body", e)
                ""
            }
        }

        /**
         * Executes a request, returning the response as a stream.
         */
        fun executeStream(request: Request): InputStream {
            val body = handle(request)?.body()
            // If the body is null, return an empty InputStream
            return body?.byteStream() ?: InputStream.nullInputStream()
        }

        /**
         * Retrieves the resource at the provided [url].
         */
        fun get(url: String) = execute(createRequest(url).build())

        /**
         * Retrieves the resource at the provided [url].
         */
        fun get(url: String, headers: Map<String, String>?): String {
            val request = createRequest(url)
            // The compiler doesn't like it when we use method references
            headers?.forEach { key, value -> request.header(key, value) }
            return execute(request.build())
        }

        /**
         * Retrieves the resource at the provided [url] as a stream.
         */
        fun getStream(url: String) = executeStream(createRequest(url).build())

        /**
         * Retrieves the resource at the provided [url] as a stream.
         */
        fun getStream(url: String, headers: Map<String, String>?): InputStream {
            val request = createRequest(url)
            // The compiler doesn't like it when we use method references
            headers?.forEach { key, value -> request.header(key, value) }
            return executeStream(request.build())
        }

        /**
         * Handles the actual execution of a request, logging any exceptions.
         */
        fun handle(request: Request): Response? {
            return try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                logger.error("Failed to execute an HTTP request", e)
                null
            }
        }

        /**
         * Performs a POST request to the [url] with the provided [body].
         */
        fun post(url: String, body: String?) = post(url, createBody(body))

        /**
         * Performs a POST request to the [url] with the provided [body].
         */
        fun post(url: String, body: RequestBody?): String {
            return execute(createRequest(url, "POST", body ?: createBody(null)).build())
        }

        /**
         * Performs a POST request to the [url] with the provided [body] and [headers].
         */
        fun post(url: String, body: RequestBody?, headers: Map<String, String>?): String {
            val request = createRequest(url, "POST", body ?: createBody(null))
            // The compiler doesn't like it when we use method references
            headers?.forEach { key, value -> request.header(key, value) }
            return execute(request.build())
        }

        /**
         * Performs a POST request to the [url] with the provided multi-part [form].
         */
        fun postForm(url: String, form: Map<String, String>?): String {
            val body = FormBody.Builder()
            // The compiler doesn't like it when we use method references
            form?.forEach { key, value -> body.add(key, value) }
            return execute(createRequest(url, "POST", body.build()).build())
        }

    }

}
