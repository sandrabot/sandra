/*
 * Copyright 2026 Avery Carroll, Logan Devecka, and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    application
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    id("com.github.gmazzo.buildconfig") version "6.1.2"
    id("io.ktor.plugin") version "3.6.0"
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

    implementation("club.minnced:jda-ktx:0.15.0")
    implementation("net.dv8tion:JDA:6.7.0") {
        exclude(module = "opus-java")
        exclude(module = "tink")
    }

    implementation("ch.qos.logback:logback-classic:1.6.4")
    implementation("io.sentry:sentry-logback:8.58.0")
    implementation("net.jodah:expiringmap:0.5.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("redis.clients:jedis:8.0.1")

    runtimeOnly(kotlin("scripting-jsr223"))
}

kotlin {
    jvmToolchain(25)
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

fun gitCommand(vararg parts: String) = providers.exec {
    commandLine("git", *parts)
}.standardOutput.asText.get().trim()
