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

package com.sandrabot.sandra

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.sandrabot.sandra.config.RedisConfig
import com.sandrabot.sandra.config.SandraConfig
import com.sandrabot.sandra.managers.CredentialManager
import com.sandrabot.sandra.managers.RedisManager
import io.sentry.Sentry
import io.sentry.dsn.Dsn
import net.dv8tion.jda.api.JDAInfo
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

fun main() {

    val code = bootstrap()
    if (code != 0) exitProcess(code)

}

fun bootstrap(): Int {

    val logger = LoggerFactory.getLogger(Sandra::class.java)
    val beginStartup = System.currentTimeMillis()

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
        JSONObject(File("config.json").readText())
    } catch (e: Exception) {
        logger.error("Failed to read config file, exiting immediately", e)
        return 1
    }

    val sandraConfig = SandraConfig(config)
    if (sandraConfig.developmentMode) {
        logger.info("Development mode has been enabled, using beta token")
    }

    // Adjust the root logger level if specified
    if (config.has("logLevel")) {
        val level = Level.toLevel(config.getString("logLevel"), Level.INFO)
        (LoggerFactory.getLogger("ROOT") as Logger).level = level
    }

    // Configure the Sentry client, if enabled
    val dsn = if (sandraConfig.sentryEnabled) {
        config.optString("dsn", null)
    } else null
    val sentry = Sentry.init(dsn ?: Dsn.DEFAULT_DSN)
    if (sandraConfig.sentryEnabled) {
        if (dsn == null) logger.warn("Sentry is enabled but the DSN was not found, using noop client")
        // stacktrace.app.packages can only be set in sentry.properties apparently
        sentry.environment = if (sandraConfig.developmentMode) "development" else "production"
        sentry.release = SandraInfo.COMMIT
    }

    // Initialize the credential manager to be used throughout the bot
    val credentials = try {
        CredentialManager(config.getJSONObject("credentials"))
    } catch (e: Exception) {
        logger.error("Failed to read credentials, exiting immediately", e)
        return 1
    }

    // Configure a redis manager to be used throughout the bot
    val redisConfig = RedisConfig(config.optJSONObject("redis") ?: JSONObject())
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

    Runtime.getRuntime().addShutdownHook(Thread({ sandra.shutdown() }, "Shutdown Hook"))
    val duration = System.currentTimeMillis() - beginStartup
    logger.info("Initialization finished in ${"%,d".format(duration)}ms")

    return 0

}
