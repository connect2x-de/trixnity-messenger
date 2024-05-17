package de.connect2x.trixnity.messenger.viewmodel.connecting

sealed interface AddMatrixAccountState {
    data object None : AddMatrixAccountState
    data object Connecting : AddMatrixAccountState
    data object Success : AddMatrixAccountState
    data class Failure(val message: String) : AddMatrixAccountState
}
