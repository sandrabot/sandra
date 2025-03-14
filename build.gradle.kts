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
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.github.gmazzo.buildconfig") version "5.5.4"
    id("io.ktor.plugin") version "3.1.1"
}

group = "com.sandrabot"
version = "5.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("io.ktor:ktor-client-okhttp")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    implementation("club.minnced:jda-ktx:0.12.0")
    implementation("net.dv8tion:JDA:5.3.0") {
        exclude(module = "opus-java")
    }

    implementation("ch.qos.logback:logback-classic:1.5.17")
    implementation("io.sentry:sentry-logback:8.3.0")
    implementation("net.jodah:expiringmap:0.5.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("redis.clients:jedis:5.2.0")

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
    buildConfigField("VERSION", provider { "${project.version}" })
    buildConfigField("COMMIT", gitCommand("rev-parse", "HEAD"))
    buildConfigField("LOCAL_CHANGES", gitCommand("diff", "--shortstat"))
    buildConfigField("BUILD_TIME", System.currentTimeMillis())
    useKotlinOutput { internalVisibility = false }
}

fun gitCommand(vararg parts: String): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", *parts)
        standardOutput = stdout
    }
    return stdout.toString("utf-8").trim()
}
