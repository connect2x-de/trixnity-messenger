package de.connect2x.trixnity.messenger.compose.view.util

import io.ktor.client.engine.HttpClientEngine

actual fun platformHttpEngine(): HttpClientEngine {
    throw Exception("tests requiring a MatrixMultiMessenger should be run as instrumented tests")
}
