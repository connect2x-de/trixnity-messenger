package de.connect2x.trixnity.messenger.notification.unifiedpush

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.update
import de.connect2x.trixnity.messenger.notification.PushNotificationProvider
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.withDiFromService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val log = KotlinLogging.logger {}

class OnNewEndpointWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        const val UNIQUE_WORK_NAME = "de.connect2x.trixnity.messenger.notification.unifiedpush.OnNewEndpointWorker"

        fun enqueueUniqueWork(
            context: Context,
            url: String,
        ) {
            val workRequest = OneTimeWorkRequestBuilder<OnNewEndpointWorker>()
                .setInputData(
                    workDataOf(
                        "url" to url,
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }
    }

    @Serializable
    data class PushEndpointMatrixGatewayDiscovery(
        @SerialName("unifiedpush") val unifiedpush: UnifiedPush? = null,
    ) {
        @Serializable
        data class UnifiedPush(
            @SerialName("gateway") val gateway: String? = null,
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun doWork(): Result {
        val pushKey = inputData.getString("url") ?: return Result.failure()
        val pushKeyAsUrl = URLBuilder("/_matrix/push/v1/notify").apply {
            val parsedEndpointUrl = parseUrl(pushKey) ?: return Result.failure()
            protocol = parsedEndpointUrl.protocol
            host = parsedEndpointUrl.host
            port = parsedEndpointUrl.port
        }.build()

        withDiFromService(context) { di ->
            val config = di.get<MatrixMessengerBaseConfiguration>()
            val httpClientConfig: HttpClientConfig<*>.() -> Unit = {
                config.httpClientConfig?.invoke(this)
                expectSuccess = false
            }
            val httpClient =
                config.httpClientEngine
                    ?.let { HttpClient(it, httpClientConfig) }
                    ?: HttpClient(httpClientConfig)

            val url = try {
                httpClient.get(pushKeyAsUrl) {
                    accept(ContentType.Application.Json)
                }.bodyAsText().let {
                    json.decodeFromString<PushEndpointMatrixGatewayDiscovery>(it)
                }
                pushKeyAsUrl.toString()
            } catch (e: Exception) {
                log.warn(e) { "Failed to fetch push endpoint discovery for $pushKey, using fallback url from config instead" }
                di.get<UnifiedPushNotificationProviderConfig>().pushUrl
            }
            val pusher = PushNotificationProvider.PusherSettings(
                pushKey = pushKey,
                url = url
            )
            val multiSettings = di.getOrNull<MatrixMultiMessengerSettingsHolder>()
            val settings = di.getOrNull<MatrixMessengerSettingsHolder>()
            if (multiSettings != null) {
                multiSettings.update<MatrixMultiMessengerNotificationProviderUnifiedPushSettings> {
                    it.copy(pusher = pusher)
                }
            } else if (settings != null) {
                settings.update<MatrixMessengerNotificationProviderUnifiedPushSettings> {
                    it.copy(pusher = pusher)
                }
            }
        }
        return Result.success()
    }
}
