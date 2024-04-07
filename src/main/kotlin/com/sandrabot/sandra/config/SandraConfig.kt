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

package com.sandrabot.sandra.config

import kotlinx.serialization.Serializable

/**
 * This class is used to configure the Sandra instance during startup.
 */
@Serializable
data class SandraConfig(

    /**
     * Configuration for redis database connections within the redis manager.
     */
    val redis: RedisConfig = RedisConfig(),

    /**
     * Configuration for tokens and secrets within the bot.
     */
    val secrets: SecretConfig = SecretConfig(),

    /**
     * Configuration that allows features to be managed on the fly.
     */
    val features: FeatureConfig = FeatureConfig(),

    /**
     * When enabled, experimental features and configurations will be used.
     * The development token will be used instead of the production token.
     * **(Default: true)**
     */
    val development: Boolean = true,

    /**
     * Determines whether debug messages should be logged to the console.
     * **(Default: false)**
     */
    val debug: Boolean = false,

    /**
     * When true, the api will be initialized using the specified port.
     * **(Default: false)**
     */
    val apiEnabled: Boolean = false,

    /**
     * When enabled, the Sentry client will be initialized with the specified DSN.
     * This is not recommended for development environments.
     * **(Default: false)**
     */
    val sentryEnabled: Boolean = false,

    /**
     * Determines whether the slash commands should be updated on startup.
     * **(Default: true)**
     */
    val commandUpdates: Boolean = true,

    /**
     * The DSN used to initialize the Sentry client.
     * **(Default: null)**
     */
    val sentryDsn: String? = null,

    /**
     * Determines which port the api will be initialized on.
     * **(Default: 41517)**
     */
    val apiPort: Int = 41517,

    /**
     * Determines the total number of shards a session will use.
     * **(Default: -1)**
     */
    val shardsTotal: Int = -1,

    )
