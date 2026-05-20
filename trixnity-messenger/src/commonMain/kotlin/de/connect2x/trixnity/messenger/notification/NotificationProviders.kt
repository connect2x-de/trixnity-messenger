package de.connect2x.trixnity.messenger.notification

import kotlin.jvm.JvmInline

@JvmInline
value class NotificationProviders private constructor(private val value: List<NotificationProvider>) :
    List<NotificationProvider> by value {
    constructor(
        providers: List<NotificationProvider>,
        getNoOpNotificationProvider: () -> NoOpNotificationProvider,
    ) : this(providers.ifEmpty { listOf(getNoOpNotificationProvider()) })
}
