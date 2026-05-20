package de.connect2x.trixnity.messenger.compose.view.util

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import okhttp3.ConnectionSpec

actual fun platformHttpEngine(): HttpClientEngine {
    return OkHttp.create { config { connectionSpecs(listOf(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS)) } }
}
