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
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("com.github.ben-manes.versions") version "0.39.0"
    id("com.github.gmazzo.buildconfig") version "3.0.3"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "com.sandrabot"
version = "5.0.0-SNAPSHOT"

// Needed for shadowJar and application since they wanted to deprecate it
setProperty("mainClassName", "com.sandrabot.sandra.MainKt")

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    listOf("stdlib", "reflect", "script-util", "script-runtime",
            "scripting-compiler-embeddable", "compiler-embeddable"
    ).forEach { implementation(kotlin(it)) }
    implementation("net.dv8tion:JDA:5.0.0-alpha.3") {
        // We don't need this because lavaplayer will always send opus for us
        exclude(module = "opus-java")
    }
    implementation("ch.qos.logback:logback-classic:1.2.9")
    implementation("io.javalin:javalin:4.1.1")
    implementation("io.ktor:ktor-client-okhttp:1.6.7")
    implementation("io.ktor:ktor-client-serialization:1.6.7")
    implementation("io.sentry:sentry-logback:5.5.2")
    implementation("net.jodah:expiringmap:0.5.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.reflections:reflections:0.10.2")
    implementation("redis.clients:jedis:4.0.0")
}

buildConfig {
    className("SandraInfo")
    val commit = runCommand(listOf("git", "rev-parse", "HEAD"))
    val changes = runCommand(listOf("git", "diff", "--shortstat"))
    buildConfigField("String", "VERSION", "\"$version\"")
    buildConfigField("String", "COMMIT", "\"$commit\"")
    buildConfigField("String", "LOCAL_CHANGES", "\"$changes\"")
    buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "17"
compileKotlin.kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"

fun runCommand(commands: List<String>): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = commands
        standardOutput = stdout
    }
    return stdout.toString("utf-8").trim()
}
