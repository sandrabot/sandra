/*
 * Copyright 2017-2021 Avery Carroll and Logan Devecka
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
    kotlin("jvm") version "1.4.21"
    id("com.github.ben-manes.versions") version "0.36.0"
    id("com.github.gmazzo.buildconfig") version "2.0.2"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

val commit = runCommand(arrayListOf("git", "rev-parse", "HEAD"))
val changes = runCommand(arrayListOf("git", "diff", "--shortstat"))

group = "com.sandrabot"
version = "5.0.0-SNAPSHOT"

// Needed for shadowJar and application since they wanted to deprecate it
setProperty("mainClassName", "com.sandrabot.sandra.MainKt")

repositories {
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    listOf("stdlib-jdk8", "reflect", "script-util", "script-runtime",
            "scripting-compiler-embeddable", "compiler-embeddable"
    ).forEach { implementation(kotlin(it)) }
    implementation("net.dv8tion:JDA:4.2.0_227") {
        // We don't need this because lavaplayer will always send opus for us
        exclude(module = "opus-java")
    }
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.beust:klaxon:5.4")
    implementation("io.javalin:javalin:3.12.0")
    implementation("io.ktor:ktor-client-core:1.5.0")
    implementation("io.ktor:ktor-client-okhttp:1.5.0")
    implementation("io.ktor:ktor-client-jackson:1.5.0")
    implementation("io.sentry:sentry-logback:1.7.30")
    implementation("me.xdrop:fuzzywuzzy:1.3.1")
    implementation("net.jodah:expiringmap:0.5.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.reflections:reflections:0.9.12")
    implementation("redis.clients:jedis:3.4.0")
}

buildConfig {
    className = "SandraInfo"
    buildConfigField("String", "VERSION", "\"$version\"")
    buildConfigField("String", "COMMIT", "\"$commit\"")
    buildConfigField("String", "LOCAL_CHANGES", "\"$changes\"")
    buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "13"

fun runCommand(commands: List<String>): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = commands
        standardOutput = stdout
    }
    return stdout.toString("utf-8").trim()
}
