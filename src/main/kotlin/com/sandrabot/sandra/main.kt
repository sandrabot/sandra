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

package com.sandrabot.sandra

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.sandrabot.sandra.config.RedisConfig
import com.sandrabot.sandra.config.SandraConfig
import com.sandrabot.sandra.managers.CredentialManager
import com.sandrabot.sandra.managers.RedisManager
import com.sandrabot.sandra.utils.getResourceAsText
import io.sentry.Sentry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import net.dv8tion.jda.api.JDAInfo
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.reflect.full.declaredMemberProperties
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

fun main(args: Array<String>): Unit = bootstrap(args).let { if (it != 0) exitProcess(it) }

fun bootstrap(args: Array<String>): Int {

    val beginStartup = System.currentTimeMillis()
    val logger = LoggerFactory.getLogger(Sandra::class.java)
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    // Print the logo and any relevant version information
    println("\n${getResourceAsText("/logo.txt")}")
    println(" | Version: ${SandraInfo.VERSION}")
    println(" | Commit: ${SandraInfo.COMMIT}")
    if (SandraInfo.LOCAL_CHANGES.isNotBlank()) {
        println("   * ${SandraInfo.LOCAL_CHANGES}")
    }
    println(" | JDA: ${JDAInfo.VERSION}\n")

    logger.info("Initializing Sandra (◕ᴗ◕✿)")

    // If an argument was provided, use it as the config file location
    val file = File(if (args.isNotEmpty()) args[0] else "config.json")
    val config = try {
        // Attempt to parse the config file if it exists
        if (file.exists()) json.decodeFromString<JsonObject>(file.readText()) else {
            // Otherwise, generate a new config file with default values
            val config = buildJsonObject {
                json.encodeToJsonElement(SandraConfig()).jsonObject.forEach { put(it.key, it.value) }
                put("redis", json.encodeToJsonElement(RedisConfig()))
                // We can't exactly provide default credentials, so we set them to empty strings instead
                putJsonObject("credentials") {
                    CredentialManager::class.declaredMemberProperties.forEach { put(it.name, "") }
                }
            }
            try {
                // Attempt to write the config file at the same location as provided
                file.writeText(json.encodeToString(config))
                logger.info("The configuration file wasn't found, one has been created for you at ${file.absolutePath}")
            } catch (t: Throwable) {
                logger.error("Failed to write default configuration file at ${file.absolutePath}", t)
            }
            return 1
        }
    } catch (t: Throwable) {
        logger.error("Failed to read configuration file at ${file.absolutePath}, exiting with error code", t)
        return 1
    }

    val sandraConfig = json.decodeFromJsonElement<SandraConfig>(config)
    if (sandraConfig.development) logger.info("Development mode is enabled, beta configurations will be used")

    // Adjust the root logger level if specified
    if (sandraConfig.logLevel != null) {
        val level = Level.toLevel(sandraConfig.logLevel, Level.INFO)
        (LoggerFactory.getLogger("ROOT") as Logger).level = level
    }

    // Configure the Sentry client, if enabled
    if (sandraConfig.sentryEnabled) Sentry.init {
        it.dsn = if (sandraConfig.sentryEnabled) sandraConfig.sentryDsn ?: "" else ""
        if (it.dsn.isNullOrEmpty()) logger.warn("Sentry is enabled but the DSN was not provided, client will be noop")
        // stacktrace.app.packages can only be set in sentry.properties apparently
        it.environment = if (sandraConfig.development) "development" else "production"
        it.release = SandraInfo.COMMIT
    }

    // Initialize the credential manager with all the tokens and secrets
    val credentials = try {
        json.decodeFromJsonElement<CredentialManager>(config["credentials"]!!)
    } catch (t: Throwable) {
        logger.error("Failed to read credentials from configuration file, exiting with error code", t)
        return 1
    }

    // Configure a redis manager to communicate with the database
    val redisConfig = try {
        json.decodeFromJsonElement(config.getOrDefault("redis", buildJsonObject {}))
    } catch (t: Throwable) {
        logger.warn("Failed to read redis configuration, attempting to continue with defaults", t)
        RedisConfig()
    }

    // Test the connection to the redis server, we don't know if it's any good until we poll a resource
    logger.info("Connecting to redis at ${redisConfig.host}:${redisConfig.port} on database ${redisConfig.database}")
    val redis = RedisManager(redisConfig)
    try {
        val beginConnection = System.currentTimeMillis()
        redis.resource.use { it.ping() }
        logger.info("Verified the redis connection in ${System.currentTimeMillis() - beginConnection}ms")
    } catch (t: Throwable) {
        logger.error("Failed to verify the redis connection, exiting with error code", t)
        return 1
    }

    // Now we have everything we need to start the bot
    val sandra = try {
        // The process will hang here until a shard signs in
        Sandra(sandraConfig, redis, credentials)
    } catch (t: Throwable) {
        logger.error("Failed to initialize Sandra, exiting with error code", t)
        return 1
    }

    // Add our own shutdown hook to gracefully close our resources
    Runtime.getRuntime().addShutdownHook(Thread(sandra::shutdown, "Shutdown Hook"))
    val duration = (System.currentTimeMillis() - beginStartup).milliseconds.toString(DurationUnit.MILLISECONDS)
    logger.info("Initialization completed in $duration (✿◠‿◠) waiting for Sandra to wake up...")

    return 0

}
