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

package com.sandrabot.sandra.config

/**
 * This class is used to configure the Sandra instance during startup.
 */
class SandraConfig {

    /**
     * Primarily determines which Discord account this session will sign into.
     * It may also be used to determine other behaviors throughout the bot.
     * When set to `true`, the beta account will be used. By using `true` as
     * the default, we prevent signing into production accounts accidentally.
     */
    var development = true

    /**
     * Determines whether the api is enabled or not.
     * When the api is disabled, it will not be started or
     * stopped automatically, however it can still be started.
     */
    var apiEnabled = true

    /**
     * Determines whether sentry is enabled or not.
     * When sentry is disabled, error and warning events will not
     * be sent to sentry, whether the DSN is present or not.
     */
    var sentryEnabled = true

    /**
     * Determines the port the api will use when started.
     */
    var apiPort = 41517

    /**
     * Determines how many shards this session should use.
     * By using `-1` as the default, JDA will use the suggested
     * amount for the account we are signing into.
     */
    var shardsTotal = -1

}
