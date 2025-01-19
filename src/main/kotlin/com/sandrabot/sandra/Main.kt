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

package com.sandrabot.sandra

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.sandrabot.sandra.config.SandraConfig
import com.sandrabot.sandra.managers.RedisManager
import com.sandrabot.sandra.utils.HTTP_CLIENT
import com.sandrabot.sandra.utils.useResourceStream
import io.sentry.Sentry
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.messages.MessageRequest
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.net.InetAddress
import java.util.*
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

private val logger = LoggerFactory.getLogger("Sandra")

fun main(args: Array<String>) = try {
    bootstrap(args)
} catch (t: Throwable) {
    logger.error("An exception occurred while starting Sandra, exiting with error code", t)
    exitProcess(1)
}

fun bootstrap(args: Array<String>) {

    val beginStartup = System.currentTimeMillis()
    // print the startup banner and some version information
    println(useResourceStream("banner.txt") { String(readBytes()) })
    println(" | Version: ${BuildInfo.VERSION}")
    println(" | Commit: ${BuildInfo.COMMIT}")
    BuildInfo.LOCAL_CHANGES.ifEmpty { null }?.let { println("   * $it") }
    println(" | JDA: ${JDAInfo.VERSION}\n")

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    // allow the user to specify a custom config file
    val configFile = File(args.firstOrNull() ?: "config.json")
    val config: SandraConfig = if (configFile.exists()) try {
        json.decodeFromString(configFile.readText())
    } catch (t: Throwable) {
        throw IllegalStateException("Failed to parse config file at ${configFile.absolutePath}", t)
    } else {
        configFile.writeText(json.encodeToString(SandraConfig()))
        throw FileNotFoundException("A new config file has been created for you at ${configFile.absolutePath}")
    }

    if (config.development) logger.info("Running in development mode, experimental features may cause instability")
    if (config.sentryEnabled && !config.sentryDsn.isNullOrBlank()) Sentry.init { options ->
        // configure the sentry client for the current environment
        options.dsn = config.sentryDsn
        options.release = BuildInfo.COMMIT
        options.serverName = InetAddress.getLocalHost().hostName
        options.environment = if (config.development) "development" else "production"
    } else logger.warn("Sentry is disabled, error reporting will not be available")
    if (config.debug) {
        logger.info("Running in debug mode, all loggers will print debug messages")
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.DEBUG
    }

    // configure and test the redis connection
    val redisManager = RedisManager(config.redis)
    val ping = measureTime { redisManager.use { ping() } }
    logger.info("Verified database connection at ${config.redis.host}:${config.redis.port}/${config.redis.database} in $ping")

    // eliminate the possibility of accidental mass mentions
    val disabled = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE)
    MessageRequest.setDefaultMentions(EnumSet.complementOf(disabled))

    // initialize the sandra instance and start the bot
    // this is a blocking call while jda signs into the first shard
    val sandra = Sandra(config, redisManager)
    val self = sandra.shards.shardCache.first().selfUser
    logger.info("Token used to sign in belongs to account: ${self.name} (${self.id})")

    val startupTime = (System.currentTimeMillis() - beginStartup).milliseconds
    logger.info("Startup completed, connecting with ${sandra.shards.shardsTotal} shards... (took $startupTime)")
    Runtime.getRuntime().addShutdownHook(Thread({ shutdownHook(sandra) }, "Shutdown Hook"))

}

private fun shutdownHook(sandra: Sandra) = with(sandra) {
    logger.info("Shutdown hook has been reached, gracefully closing resources...")

    // close all resources and shutdown the shard manager
    // order is important here to ensure data integrity
    try {
        // disable any auxiliary services first
        botList.shutdown()
        blocklist.shutdown()
        subscriptions.shutdown()

        // stop accepting new requests and sign out from discord
        api?.shutdown()
        shards.shutdown()
        HTTP_CLIENT.close()

        // save config data before closing the redis connection
        config.shutdown()
        redis.shutdown()
    } catch (t: Throwable) {
        logger.error("An exception occurred while shutting down, halting immediately", t)
        Runtime.getRuntime().halt(1)
    }
}
