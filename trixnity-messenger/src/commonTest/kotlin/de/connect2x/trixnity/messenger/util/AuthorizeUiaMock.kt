package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AuthorizeUiaMock(
    coroutineScope: CoroutineScope,
    private val delegate: AuthorizeUia = AuthorizeUiaImpl()
) : AuthorizeUia by delegate {
    val onRequestFlowState = onRequestFlow.stateIn(coroutineScope, SharingStarted.Eagerly, null)
}
