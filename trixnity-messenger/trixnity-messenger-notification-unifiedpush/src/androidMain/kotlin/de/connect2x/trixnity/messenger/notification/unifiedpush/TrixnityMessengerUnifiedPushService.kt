package de.connect2x.trixnity.messenger.notification.unifiedpush

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

private val log = KotlinLogging.logger { }

class TrixnityMessengerUnifiedPushService : PushService() {

    override fun onCreate() {
        log.trace { "onCreate" }
        super.onCreate()
    }

    override fun onDestroy() {
        log.trace { "onDestroy" }
        super.onDestroy()
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Serializable
    data class UnifiedPushMessage(
        @SerialName("notification") val notification: Notification,
    ) {
        @Serializable
        data class Notification(
            @SerialName("devices") val devices: List<Device> = listOf(),
            @SerialName("room_id") val roomId: String,
            @SerialName("event_id") val eventId: String? = null,
        ) {
            @Serializable
            data class Device(
                @SerialName("app_id") val appId: String,
                @SerialName("data") val data: Data,
            ) {
                @Serializable
                data class Data(
                    @SerialName("default_payload") val defaultPayload: DefaultPayload? = null,
                ) {
                    @Serializable
                    data class DefaultPayload(
                        @SerialName("profile") val profile: String? = null,
                        @SerialName("account") val account: String? = null,
                    )
                }
            }
        }
    }

    override fun onMessage(message: PushMessage, instance: String) {
        val notifications = try {
            json.decodeFromString<UnifiedPushMessage>(message.content.decodeToString()).notification
        } catch (e: Exception) {
            log.warn(e) { "Failed to decode push message: ${message.content.decodeToString()}" }
            return
        }
        val data = notifications.devices.firstOrNull()?.data
        OnMessageReceivedWorker.enqueueWork(
            context = applicationContext,
            profile = data?.defaultPayload?.profile,
            account = data?.defaultPayload?.account,
            roomId = notifications.roomId,
            eventId = notifications.eventId,
        )
    }

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        log.trace { "onNewToken" }
        OnNewEndpointWorker.enqueueUniqueWork(
            context = applicationContext, url = endpoint.url
        )
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        log.warn { "onRegistrationFailed: $reason" }
        UnifiedPush.tryUseCurrentOrDefaultDistributor(context = applicationContext) { success ->
            if (!success) {
                log.error { "Could not retry initialize UnifiedPush" }
            } else {
                UnifiedPush.register(context = applicationContext)
            }
        }
    }

    override fun onUnregistered(instance: String) {
        log.debug { "onUnregistered" }
    }
}
