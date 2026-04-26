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

package com.sandrabot.sandra.config

import com.sandrabot.sandra.constants.FeatureFlag
import kotlinx.serialization.Serializable

/**
 * Object used to configure various features without necessitating hard-coded values.
 */
@Serializable
data class FeatureConfig(

    /**
     * A set of features [FeatureFlag] that should be disabled globally.
     */
    val disabledFeatures: Set<FeatureFlag> = setOf(),

    /**
     * The channel where feedback messages should be sent.
     * **(Default: 0)**
     */
    val feedbackChannel: Long = 0L,

    )
