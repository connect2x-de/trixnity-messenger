package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.internal.uia.UIADestination
import de.connect2x.trixnity.messenger.internal.uia.UIANavigator
import de.connect2x.trixnity.messenger.internal.uia.UIASessionHolder
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

internal interface UIAWorker : Worker

internal fun UIAWorker(authorizeUia: AuthorizeUia, sessions: UIASessionHolder, navigator: UIANavigator): UIAWorker {
    return UIAWorkerImpl(authorizeUia, sessions, navigator)
}

private class UIAWorkerImpl(
    private val authorizeUia: AuthorizeUia,
    private val sessions: UIASessionHolder,
    private val navigator: UIANavigator,
) : UIAWorker {

    override suspend fun doWork() {
        authorizeUia.onRequestFlow.collectLatest { params ->
            val session = sessions.open()
            try {
                navigator.navigate(
                    UIADestination.Confirm(confirmationMessage = params.confirmationMessage, action = params.action)
                )
                params.onResult(session.awaitResult())
            } finally {
                sessions.close()
                withContext(NonCancellable) { navigator.navigate(UIADestination.None) }
            }
        }
    }
}
