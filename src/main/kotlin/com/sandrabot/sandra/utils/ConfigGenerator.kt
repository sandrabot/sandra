/*
 *    Copyright 2020 Joseph Sohn
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
package com.sandrabot.sandra.utils

import com.beust.klaxon.*
import com.sandrabot.sandra.Sandra
import org.slf4j.LoggerFactory
import java.io.File


class ConfigGenerator{
    private val logger = LoggerFactory.getLogger(ConfigGenerator::class.java)
    private fun editJson(inputs: JsonObject){
        for (keys in inputs.keys){
            when{
                inputs[keys]?.javaClass?.name === "com.beust.klaxon.JsonObject" -> {
                    println("Include ${keys.toString()}? Yes/No")
                    loop@ while (true) {
                        when (readLine()!!.toLowerCase()) {
                            "y", "ye", "yes" -> {
                                editJson(inputs[keys] as JsonObject)
                                break@loop
                            }
                            "n","no"-> break@loop
                            else-> println("Please enter Yes or No.\n")
                        }
                    }
                }
                inputs[keys]?.javaClass?.name === null -> putIntoJsonObject(inputs,keys,"Null", "Null")
                inputs[keys]?.javaClass?.name === "java.lang.String" -> putIntoJsonObject(inputs,keys,"String",inputs[keys].toString())
                inputs[keys]?.javaClass?.name === "java.lang.Integer" -> putIntoJsonObject(inputs,keys,"Int",inputs[keys].toString())
            }
        }
    }
    private fun putIntoJsonObject (jsonObject: JsonObject,keyname: String, valueDefaultType: String,valueDefaultValue: String){
        println("$keyname? (Default: $valueDefaultValue)")
        jsonObject.put(keyname, when ( val input = readLine()!! ){
                "" -> {
                    when{
                        valueDefaultType === "Srting" -> valueDefaultValue
                        valueDefaultType === "Null" -> null
                        valueDefaultType === "Int" -> valueDefaultValue.toInt()
                        else -> valueDefaultValue
                    }
                }
                else -> input
            }
        )
    }
    fun generateConfig() {
        println("Generating Config. (Leave blank for default value)")
        val parser = Parser.default()
        val configTemplateString: StringBuilder = java.lang.StringBuilder(Sandra::class.java.getResource("/configTemplate.json").readText())
        val configTemplate: JsonObject = parser.parse(configTemplateString) as JsonObject
        editJson(configTemplate)
        File("config.json").writeText(configTemplate.toJsonString(true))
        logger.info("Config generated in "+File("config.json").absolutePath)
    }
}

