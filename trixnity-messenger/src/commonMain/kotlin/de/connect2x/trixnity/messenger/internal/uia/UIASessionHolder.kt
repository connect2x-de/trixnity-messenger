package de.connect2x.trixnity.messenger.internal.uia

import kotlin.concurrent.atomics.AtomicReference

internal interface UIASessionHolder {
    fun open(): UIASession

    fun current(): UIASession?

    fun close()
}

internal fun UIASessionHolder(): UIASessionHolder {
    return UIASessionHolderImpl()
}

private class UIASessionHolderImpl : UIASessionHolder {
    private val sessionOrNull = AtomicReference<UIASession?>(null)

    override fun open(): UIASession {
        val session = UIASession()
        check(sessionOrNull.compareAndSet(null, session)) { "previous session was not closed properly" }
        return session
    }

    override fun current(): UIASession? {
        return sessionOrNull.load()
    }

    override fun close() {
        sessionOrNull.store(null)
    }
}
