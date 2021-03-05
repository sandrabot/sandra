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

package com.sandrabot.sandra

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.beust.klaxon.json
import com.sandrabot.sandra.config.RedisConfig
import com.sandrabot.sandra.config.SandraConfig
import com.sandrabot.sandra.managers.CredentialManager
import com.sandrabot.sandra.managers.RedisManager
import io.sentry.Sentry
import io.sentry.dsn.Dsn
import net.dv8tion.jda.api.JDAInfo
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.reflect.full.declaredMemberProperties
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    val code = bootstrap(args)
    if (code != 0) exitProcess(code)

}

fun bootstrap(args: Array<String>): Int {

    val beginStartup = System.currentTimeMillis()
    val logger = LoggerFactory.getLogger(Sandra::class.java)

    // Print the logo and any relevant version information
    println("\n${Sandra::class.java.getResource("/logo.txt").readText()}")
    println(" | Version: ${SandraInfo.VERSION}")
    println(" | Commit: ${SandraInfo.COMMIT}")
    if (SandraInfo.LOCAL_CHANGES.isNotBlank()) {
        println("   * ${SandraInfo.LOCAL_CHANGES}")
    }
    println(" | JDA: ${JDAInfo.VERSION}\n")

    logger.info("Initializing Sandra (◕ᴗ◕✿)")

    // Read the file containing all our options
    val config = try {
        val fileName = if (args.isNotEmpty()) args[0] else "config.json"
        val file = File(fileName)
        if (!file.exists()) {
            // Generate a configuration file using default values
            val config = json {
                val sandraConfig = SandraConfig()
                obj(SandraConfig::class.declaredMemberProperties.map { it.name to it.get(sandraConfig) })
            }
            config["redis"] = json {
                val redisConfig = RedisConfig()
                obj(RedisConfig::class.declaredMemberProperties.map { it.name to it.get(redisConfig) })
            }
            // We can't exactly provide default credentials, so we set them to empty strings instead
            config["credentials"] = json { obj(CredentialManager::class.declaredMemberProperties.map { it.name to "" }) }
            try {
                file.writeText(config.toJsonString(prettyPrint = true))
                logger.info("The configuration file wasn't found, one has been created for you at ${file.absolutePath}")
            } catch (e: Exception) {
                logger.error("Failed to write default configuration file at ${file.absolutePath}", e)
            }
            return 1
        }
        val obj = Parser.default().parse(fileName)
        if (obj is JsonObject) obj else {
            throw IllegalArgumentException("Configuration file is improperly formatted")
        }
    } catch (e: Exception) {
        logger.error("Failed to read configuration file, exiting immediately", e)
        return 1
    }

    val sandraConfig = Klaxon().parseFromJsonObject<SandraConfig>(config)!!
    if (sandraConfig.development) {
        logger.info("Development mode has been enabled, using beta token")
    }

    // Adjust the root logger level if specified
    if (sandraConfig.logLevel != null) {
        val level = Level.toLevel(sandraConfig.logLevel, Level.INFO)
        (LoggerFactory.getLogger("ROOT") as Logger).level = level
    }

    // Configure the Sentry client, if enabled
    val dsn = if (sandraConfig.sentryEnabled) sandraConfig.sentryDsn else null
    val sentry = Sentry.init(dsn ?: Dsn.DEFAULT_DSN)
    if (sandraConfig.sentryEnabled) {
        if (dsn == null) logger.warn("Sentry is enabled but the DSN was not found, using noop client")
        // stacktrace.app.packages can only be set in sentry.properties apparently
        sentry.environment = if (sandraConfig.development) "development" else "production"
        sentry.release = SandraInfo.COMMIT
    }

    // Initialize the credential manager to be used throughout the bot
    val credentials = try {
        val obj = config["credentials"]
        if (obj != null && obj is JsonObject) {
            Klaxon().parseFromJsonObject<CredentialManager>(obj)!!
        } else throw IllegalArgumentException("Credentials are missing or improperly formatted")
    } catch (e: Exception) {
        logger.error("Failed to read credentials, exiting immediately", e)
        return 1
    }

    // Configure a redis manager to be used throughout the bot
    val redisConfig = try {
        val obj = config["redis"] ?: JsonObject()
        if (obj is JsonObject) {
            Klaxon().parseFromJsonObject<RedisConfig>(obj)!!
        } else throw IllegalArgumentException("Redis settings are improperly formatted")
    } catch (e: Exception) {
        logger.warn("Failed to read redis configuration, continuing with defaults", e)
        RedisConfig()
    }
    logger.info("Connecting to redis at ${redisConfig.host}:${redisConfig.port} on database ${redisConfig.database}")
    val redis = RedisManager(redisConfig)

    // Test the connection to the redis server, we don't know if it's any good until we poll a resource
    try {
        val beginConnection = System.currentTimeMillis()
        redis.resource.use { it.ping() }
        logger.info("Verified the redis connection in ${System.currentTimeMillis() - beginConnection}ms")
    } catch (e: Exception) {
        logger.error("Failed to verify the redis connection, exiting immediately", e)
        return 1
    }

    // Now we have everything we need to start the bot
    val sandra = try {
        Sandra(sandraConfig, redis, credentials)
    } catch (e: Exception) {
        logger.error("Failed to initialize Sandra, exiting immediately", e)
        return 1
    }

    Runtime.getRuntime().addShutdownHook(Thread(sandra::shutdown, "Shutdown Hook"))
    val duration = System.currentTimeMillis() - beginStartup
    logger.info("Initialization finished in ${"%,d".format(duration)}ms")

    return 0

}
