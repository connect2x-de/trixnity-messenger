package de.connect2x.trixnity.messenger.compose.view.util

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpEngine(): HttpClientEngine {
    return Darwin.create { }
}
