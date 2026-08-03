package de.connect2x.trixnity.messenger.internal.uia

import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import kotlinx.coroutines.CompletableDeferred

internal interface UIASession {
    fun complete(result: AuthorizeUiaResult<*>)

    suspend fun awaitResult(): AuthorizeUiaResult<*>
}

internal fun UIASession(): UIASession {
    return UIASessionImpl()
}

private class UIASessionImpl : UIASession {
    private val result = CompletableDeferred<AuthorizeUiaResult<*>>()

    override fun complete(result: AuthorizeUiaResult<*>) {
        this.result.complete(result)
    }

    override suspend fun awaitResult(): AuthorizeUiaResult<*> = result.await()
}
