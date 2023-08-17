package de.connect2x.trixnity.messenger.viewmodel.connecting

sealed interface LoginState {
    object None : LoginState
    object Connecting : LoginState
    object Success : LoginState
    data class Failure(val message: String) : LoginState
}