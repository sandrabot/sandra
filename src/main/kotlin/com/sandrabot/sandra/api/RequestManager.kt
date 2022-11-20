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

package com.sandrabot.sandra.api

import com.sandrabot.sandra.Sandra
import com.sandrabot.sandra.constants.Constants
import com.sandrabot.sandra.entities.EndpointHandler
import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.HttpResponseException
import io.javalin.http.HttpStatus
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration.Companion.milliseconds

/**
 * Responsible for receiving and responding to all incoming API requests.
 */
class RequestManager(private val sandra: Sandra) {

    private val javalinServlet: Javalin = Javalin.create { config ->
        config.showJavalinBanner = false
        config.http.prefer405over404 = true
        config.http.defaultContentType = ContentType.JSON
        config.routing.ignoreTrailingSlashes = true
        if (sandra.settings.development) config.plugins.enableDevLogging()
        config.plugins.enableRedirectToLowercasePaths()
        config.contextResolver.ip = { context ->
            // resolve the original ip address if request was forwarded
            context.header("Forwarded")?.apply {
                substringAfter("for=").substringBefore(",").substringBefore(";")
            } ?: context.req().remoteAddr
        }
        config.requestLogger.http { it, ms -> logger.info("${it.method()} ${it.path()} ${it.ip()} ${it.userAgent()} ${ms.roundToInt().milliseconds}ms") }
    }

    init {
        // map all possible error status values for custom responses
        HttpStatus.values().filter { it.code in 400..599 }.forEach { status ->
            javalinServlet.error(status) { jsonResult(it, status, false, "message" to status.message) }
        }
        // map all possible exceptions for custom responses
        javalinServlet.exception(HttpResponseException::class.java) { e, context ->
            logger.debug("Request handler for ${context.url()} threw response exception while handling request", e)
            jsonResult(context, HttpStatus.forStatus(e.status), false, "message" to (e.message ?: "Unknown"))
        }
        javalinServlet.exception(Exception::class.java) { e, context ->
            logger.error("Request handler for ${context.url()} threw exception while handling request", e)
            jsonResult(context, HttpStatus.INTERNAL_SERVER_ERROR, false, "message" to "Internal server error")
        }
        // dynamically load the endpoint handlers using reflections
        val reflections = Reflections("com.sandrabot.sandra.api.handlers").getSubTypesOf(EndpointHandler::class.java)
        reflections.mapNotNull { handlerClass ->
            try {
                handlerClass.kotlin.primaryConstructor?.call(sandra)
            } catch (t: Throwable) {
                logger.error("An exception occurred while loading endpoint handler $handlerClass", t)
                null
            }
        }.forEach { javalinServlet.addHandler(it.type, it.path, it) }
    }

    fun start() {
        javalinServlet.start(sandra.settings.apiPort)
    }

    fun shutdown() {
        javalinServlet.close()
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(RequestManager::class.java)
    }

}

internal fun jsonResult(
    context: Context, status: HttpStatus = HttpStatus.OK, success: Boolean = true, vararg elements: Pair<String, Any>
) = mutableMapOf(*elements, "status" to status.code, "success" to success, "version" to Constants.VERSION).let {
    context.status(status).json(it)
}
