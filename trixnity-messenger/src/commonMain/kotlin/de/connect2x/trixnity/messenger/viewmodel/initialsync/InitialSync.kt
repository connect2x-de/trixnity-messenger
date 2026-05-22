package de.connect2x.trixnity.messenger.viewmodel.initialsync

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.MatrixClient

/**
 * Since the initial sync can take some time due to thousands of events and poor network connection, it should be
 * running in the background, even if the app is paused or destroyed.
 *
 * On Desktop, this is not relevant (either the app is running and syncing or it is stopped), but on Android the initial
 * sync can be performed in the background via a Service, so that the sync is performed even if the device's screen is
 * locked, etc.
 *
 * On Desktop, when the scope of the caller (a view model) is ended, the app in most cases is ended as well -> it is OK
 * to cancel the initial sync in this case as the JVM is not running anymore
 */
interface RunInitialSync {
    suspend operator fun invoke(matrixClient: MatrixClient): Boolean
}

object RunInitialSyncImpl : RunInitialSync {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSyncImpl")

    override suspend operator fun invoke(matrixClient: MatrixClient): Boolean =
        matrixClient
            .syncOnce { true }
            .getOrElse {
                log.error(it) { "cannot perform initial sync" }
                false
            }
}
