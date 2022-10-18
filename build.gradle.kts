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

import org.apache.commons.io.output.ByteArrayOutputStream
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    application
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("com.github.gmazzo.buildconfig") version "3.1.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.sandrabot"
version = "5.0.0-SNAPSHOT"

// Defines the program entry point for shadowJar and application
setProperty("mainClassName", "com.sandrabot.sandra.MainKt")

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    listOf(
        "stdlib", "reflect", "script-util", "script-runtime", "scripting-compiler-embeddable", "compiler-embeddable"
    ).forEach { implementation(kotlin(it)) }
    implementation("net.dv8tion:JDA:5.0.0-alpha.21") {
        // We don't need this because lavaplayer will always send opus for us
        exclude(module = "opus-java")
    }
    implementation("ch.qos.logback:logback-classic:1.4.3")
    implementation("com.github.minndevelopment:jda-ktx:fc7d7de")
    implementation("io.javalin:javalin:5.0.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.1.2")
    implementation("io.ktor:ktor-client-okhttp:2.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.2")
    implementation("io.sentry:sentry-logback:6.4.2")
    implementation("net.jodah:expiringmap:0.5.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("redis.clients:jedis:4.2.3")
}

buildConfig {
    className("SandraInfo")
    val commit = runCommand("git", "rev-parse", "HEAD")
    val changes = runCommand("git", "diff", "--shortstat")
    buildConfigField("String", "VERSION", "\"$version\"")
    buildConfigField("String", "COMMIT", "\"$commit\"")
    buildConfigField("String", "LOCAL_CHANGES", "\"$changes\"")
    buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "17"
compileKotlin.kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"

fun runCommand(vararg parts: String): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = parts.asList()
        standardOutput = stdout
    }
    return stdout.toString("utf-8").trim()
}
