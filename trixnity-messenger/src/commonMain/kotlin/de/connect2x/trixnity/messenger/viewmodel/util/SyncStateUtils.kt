package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.clientserverapi.client.SyncState

fun SyncState.hasConnection(): Boolean {
    return when (this) {
        SyncState.RUNNING -> true
        SyncState.INITIAL_SYNC,
        SyncState.STARTED,
        SyncState.ERROR,
        SyncState.TIMEOUT,
        SyncState.STOPPED -> false
    }
}
