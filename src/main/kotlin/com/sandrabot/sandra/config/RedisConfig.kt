/*
 * Copyright 2017-2020 Avery Clifton and Logan Devecka
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

/**
 * This class is used to configure the redis manager.
 */
class RedisConfig {

    /**
     * The hostname where the redis server is running.
     */
    var host = "localhost"

    /**
     * The password used to authenticate connections. Optional.
     */
    var password: String? = null

    /**
     * The port the redis server is running on.
     */
    var port = 6379

    /**
     * The timeout for redis commands in milliseconds.
     */
    var timeout = 2000

    /**
     * The database to be used.
     */
    var database = 0

}
