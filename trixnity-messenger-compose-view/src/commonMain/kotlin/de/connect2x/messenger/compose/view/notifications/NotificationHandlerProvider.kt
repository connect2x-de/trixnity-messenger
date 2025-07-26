package de.connect2x.messenger.compose.view.notifications

import co.touchlab.stately.collections.SharedHashMap
import de.connect2x.sysnotify.NotificationHandler

interface NotificationHandlerProvider {
    companion object {
        const val GLOBAL: String = "global" // The subId of the global NotificationHandler instance

        inline fun lazy(crossinline block: (String) -> NotificationHandler): NotificationHandlerProvider {
            return object : NotificationHandlerProvider {
                private val instances: SharedHashMap<String, NotificationHandler> = SharedHashMap()
                override fun invoke(subId: String): NotificationHandler {
                    return instances.getOrPut(subId) { block(subId) }
                }

                override fun closeAll() {
                    instances.values.forEach(NotificationHandler::close)
                }
            }
        }

        fun of(handler: NotificationHandler): NotificationHandlerProvider {
            return object : NotificationHandlerProvider {
                override fun invoke(subId: String): NotificationHandler = handler
                override fun closeAll() = handler.close()
            }
        }
    }

    operator fun invoke(subId: String): NotificationHandler

    fun closeAll()
}
