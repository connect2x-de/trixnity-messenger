package de.connect2x.trixnity.messenger.viewmodel.initialsync

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import net.folivo.trixnity.client.MatrixClient

private val log = KotlinLogging.logger { }

object InitialSync {
    suspend fun run(matrixClient: MatrixClient): Boolean {
        return matrixClient.syncOnce {
            true
        }.getOrElse {
            log.error(it) { "cannot perform initial sync" }
            false
        }
    }
}

/**
 * Since the initial sync (small and the big, "real" initial sync) can take some time due to thousands of events and
 * poor network connection, it should be running in the background, even if the app is paused or destroyed.
 *
 * On Desktop, this is not relevant (either the app is running and syncing or it is stopped), but on Android the initial
 * sync can be performed in the background via a Service, so that the sync is performed even if the device's screen is
 * locked, etc.
 *
 */
interface RunInitialSync {
    operator fun invoke(accountName: String): Flow<Boolean>
}