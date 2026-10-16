package de.connect2x.livekitnity.api.network

import kotlin.time.Duration

public data class ReconnectContext(
    val retryCount: Int,
    val elapsedTime: Duration,
)
