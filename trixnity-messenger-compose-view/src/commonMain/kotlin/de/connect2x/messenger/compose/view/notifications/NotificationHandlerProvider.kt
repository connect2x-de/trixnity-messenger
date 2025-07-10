package de.connect2x.messenger.compose.view.notifications

import de.connect2x.sysnotify.NotificationHandler

fun interface NotificationHandlerProvider {
    companion object {
        const val GLOBAL: String = "global" // The subId of the global NotificationHandler instance

        inline fun lazy(crossinline block: (String) -> NotificationHandler): NotificationHandlerProvider {
            return object : NotificationHandlerProvider {
                private var instance: NotificationHandler? = null
                override fun invoke(subId: String): NotificationHandler {
                    if (instance == null) {
                        instance = block(subId)
                    }
                    return requireNotNull(instance) { "Could not create NotificationHandler for sub-ID '$subId'" }
                }
            }
        }

        fun of(handler: NotificationHandler): NotificationHandlerProvider {
            return NotificationHandlerProvider { handler }
        }
    }

    operator fun invoke(subId: String): NotificationHandler
}
