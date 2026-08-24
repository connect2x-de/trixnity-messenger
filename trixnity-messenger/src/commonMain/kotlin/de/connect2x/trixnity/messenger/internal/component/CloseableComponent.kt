package de.connect2x.trixnity.messenger.internal.component

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
interface CloseableComponent<T> : AutoCloseable {
    val value: T
}

internal fun <T> CloseableComponent(value: T, onClose: () -> Unit): CloseableComponent<T> {
    return CloseableComponentImpl(value = value, onClose = onClose)
}

private class CloseableComponentImpl<T>(override val value: T, private val onClose: () -> Unit) :
    CloseableComponent<T> {
    override fun close() {
        onClose()
    }
}
