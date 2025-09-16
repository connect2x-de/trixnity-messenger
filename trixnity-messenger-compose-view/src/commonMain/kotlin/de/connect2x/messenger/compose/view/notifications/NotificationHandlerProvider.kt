package de.connect2x.messenger.compose.view.notifications

import de.connect2x.sysnotify.NotificationHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlin.concurrent.atomics.ExperimentalAtomicApi

interface NotificationHandlerProvider : AutoCloseable {
    companion object {
        const val GLOBAL: String = "global" // The subId of the global NotificationHandler instance

        @OptIn(ExperimentalAtomicApi::class)
        inline fun lazy(crossinline block: (String) -> NotificationHandler): NotificationHandlerProvider {
            return object : NotificationHandlerProvider {
                private val instances = MutableStateFlow(mapOf<String, Lazy<NotificationHandler>>())
                override fun invoke(subId: String): NotificationHandler {
                    return checkNotNull(
                        instances.updateAndGet {
                            if (it.contains(subId)) it
                            else it + (subId to kotlin.lazy { block(subId) })
                        }[subId]?.value
                    )
                }

                override fun close() {
                    instances.value.values.forEach { it.value.close() }
                }
            }
        }

        fun of(handler: NotificationHandler): NotificationHandlerProvider {
            return object : NotificationHandlerProvider {
                override fun invoke(subId: String): NotificationHandler = handler
                override fun close() = handler.close()
            }
        }
    }

    operator fun invoke(subId: String): NotificationHandler
}
