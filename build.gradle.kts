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

import org.apache.commons.io.output.ByteArrayOutputStream
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    application
    kotlin("jvm") version "1.3.71"
    id("com.github.gmazzo.buildconfig") version "1.7.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

val commit = runCommand(arrayListOf("git", "rev-parse", "HEAD"))
val changes = runCommand(arrayListOf("git", "diff", "--shortstat"))

version = "5.0.0-SNAPSHOT"
group = "com.sandrabot"

application {
    mainClassName = "com.sandrabot.sandra.MainKt"
}

repositories {
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("net.dv8tion:JDA:4.1.1_134") {
        // We don't need this because Lavaplayer will always sends opus for us
        exclude(module = "opus-java")
    }
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.sentry:sentry-logback:1.7.30")
    implementation("org.json:json:20190722")
    implementation("redis.clients:jedis:3.2.0")
}

buildConfig {
    className = "SandraInfo"
    buildConfigField("String", "VERSION", "\"$version\"")
    buildConfigField("String", "COMMIT", "\"$commit\"")
    buildConfigField("String", "LOCAL_CHANGES", "\"$changes\"")
    buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "13"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "13"
}

fun runCommand(commands: List<String>): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = commands
        standardOutput = stdout
    }
    return stdout.toString("utf-8").trim()
}
