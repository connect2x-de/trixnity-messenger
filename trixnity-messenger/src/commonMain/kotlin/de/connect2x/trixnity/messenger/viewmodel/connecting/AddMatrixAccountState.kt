package de.connect2x.trixnity.messenger.viewmodel.connecting

sealed interface AddMatrixAccountState {
    object None : AddMatrixAccountState
    object Connecting : AddMatrixAccountState
    object Success : AddMatrixAccountState
    data class Failure(val message: String) : AddMatrixAccountState
}