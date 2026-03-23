package de.connect2x.trixnity.messenger.compose.view.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.messenger.compose.view.PlatformType
import de.connect2x.trixnity.messenger.compose.view.platformType
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private val logger = Logger("de.connect2x.trixnity.messenger.compose.view.util.SynapseClientKt")

// to create new Matrix users
fun synapseClient(engine: HttpClientEngine) =
    HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
        defaultRequest {
            url(if (platformType() == PlatformType.ANDROID) "http://10.0.2.2:8008" else "http://docker:8008")
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }

        install(HttpRequestRetry) {
            maxRetries = 2
            retryIf { request, response ->
                val result = !response.status.isSuccess()
                if (result) logger.warn { "retrying ${request.url.encodedPath}, $response" }
                result
            }
            retryOnExceptionIf { request, cause ->
                logger.warn(cause) { "retrying ${request.url.encodedPath}" }
                true
            }
            delayMillis { retry ->
                retry * 3_000L
            }
        }
    }

