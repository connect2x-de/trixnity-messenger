package de.connect2x.trixnity.messenger.viewmodel.uia

sealed interface UIAReaction {
    object DoNothing : UIAReaction
    data class ShowLogin(val response: UIAResponse) : UIAReaction
    data class UnexpectedError(val error: String?) : UIAReaction
}

