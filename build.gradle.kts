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

import java.io.ByteArrayOutputStream

plugins {
    application
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("com.github.gmazzo.buildconfig") version "5.3.5"
    id("io.ktor.plugin") version "2.3.9"
}

group = "com.sandrabot"
version = "5.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("io.ktor:ktor-client-okhttp")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    implementation("net.dv8tion:JDA:5.0.0-beta.23")
    implementation("com.github.minndevelopment:jda-ktx:78dbf82")

    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("io.sentry:sentry-logback:7.8.0")
    implementation("net.jodah:expiringmap:0.5.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.reflections:reflections:0.10.2")
    implementation("redis.clients:jedis:5.1.2")

    runtimeOnly(kotlin("scripting-jsr223"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.sandrabot.sandra.MainKt")
}

buildConfig {
    className("BuildInfo")
    val commit = executeCommand("git", "rev-parse", "HEAD")
    val localChanges = executeCommand("git", "diff", "--shortstat")
    buildConfigField("String", "VERSION", "\"$version\"")
    buildConfigField("String", "COMMIT", "\"$commit\"")
    buildConfigField("String", "LOCAL_CHANGES", "\"$localChanges\"")
    buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
}

fun executeCommand(vararg parts: String): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = parts.asList()
        standardOutput = stdout
    }
    return stdout.toString("utf-8").trim()
}
