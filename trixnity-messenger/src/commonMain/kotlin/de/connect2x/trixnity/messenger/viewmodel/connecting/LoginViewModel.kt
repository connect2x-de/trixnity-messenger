package de.connect2x.trixnity.messenger.viewmodel.connecting

import kotlinx.coroutines.flow.StateFlow

interface LoginViewModel {
    val isFirstMatrixClient: StateFlow<Boolean?>
    val serverUrl: String
}
