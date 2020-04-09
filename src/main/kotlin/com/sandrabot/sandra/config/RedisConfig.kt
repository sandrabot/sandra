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

import org.json.JSONObject
import redis.clients.jedis.Protocol

class RedisConfig(data: JSONObject) {

    var host: String = data.optString("host", Protocol.DEFAULT_HOST)
    var password: String? = data.optString("password", null)

    var port = data.optInt("port", Protocol.DEFAULT_PORT)
    var timeout = data.optInt("timeout", Protocol.DEFAULT_TIMEOUT)
    var database = data.optInt("database", Protocol.DEFAULT_DATABASE)

}
