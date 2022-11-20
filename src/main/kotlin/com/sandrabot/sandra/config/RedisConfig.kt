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

package com.sandrabot.sandra.config

import kotlinx.serialization.Serializable

/**
 * Object used to configure database connections within the redis manager.
 */
@Serializable
data class RedisConfig(

    /**
     * Hostname of the redis server location.
     * **(Default: localhost)**
     */
    val host: String = "localhost",

    /**
     * Password used to authenticate with the redis server. Optional.
     * **(Default: null)**
     */
    val password: String? = null,

    /**
     * Port that the redis server is listening on.
     * **(Default: 6379)**
     */
    val port: Int = 6379,

    /**
     * Timeout in milliseconds for redis connections.
     * **(Default: 2000)**
     */
    val timeout: Int = 2000,

    /**
     * Database number to use for the redis connection.
     * **(Default: 0)**
     */
    val database: Int = 0

)
