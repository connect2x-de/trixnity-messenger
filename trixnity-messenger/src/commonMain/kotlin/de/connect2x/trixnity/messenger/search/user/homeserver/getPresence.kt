package de.connect2x.trixnity.messenger.search.user.homeserver

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.Presence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

fun getPresence(userId: UserId, matrixClient: MatrixClient, coroutineScope: CoroutineScope): StateFlow<Presence?> {
    return matrixClient.user
        .getPresence(userId)
        .map { it?.presence }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}
