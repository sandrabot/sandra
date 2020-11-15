/*
 * Copyright 2017-2020 Avery Clifton and Logan Devecka
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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.sandrabot.sandra.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.concurrent.Task
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resumeWithException

/**
 * This file contains utility methods written
 * by MinnDevelopment in their jda-ktx project
 */

suspend fun <T> CompletableFuture<T>.await() = suspendCancellableCoroutine<T> {
    it.invokeOnCancellation { cancel(true) }
    whenComplete { result, error ->
        when {
            error != null -> it.resumeWithException(error)
            else -> it.resume(result) {}
        }
    }
}

suspend fun <T> RestAction<T>.await(): T = submit().await()

suspend fun <T> Task<T>.await() = suspendCancellableCoroutine<T> {
    it.invokeOnCancellation { cancel() }
    onSuccess { result -> it.resume(result) {} }
    onError { error -> it.resumeWithException(error) }
}
