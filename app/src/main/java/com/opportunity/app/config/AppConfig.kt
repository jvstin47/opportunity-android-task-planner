package com.opportunity.app.config

import com.opportunity.app.BuildConfig

/**
 * Centralized environment and build configuration boundary.
 *
 * All BuildConfig / local.properties values are accessed here rather than scattered
 * across network clients, AI helpers, and UI screens.
 */
object AppConfig {
    val anthropicApiKey: String
        get() = BuildConfig.ANTHROPIC_API_KEY

    val syncBaseUrl: String
        get() = BuildConfig.OPPORTUNITY_SYNC_BASE_URL.ifBlank { "http://10.0.2.2:4000/" }

    val versionName: String
        get() = BuildConfig.VERSION_NAME

    val isDebug: Boolean
        get() = BuildConfig.DEBUG

    val isAiConfigured: Boolean
        get() = anthropicApiKey.isNotBlank()
}
