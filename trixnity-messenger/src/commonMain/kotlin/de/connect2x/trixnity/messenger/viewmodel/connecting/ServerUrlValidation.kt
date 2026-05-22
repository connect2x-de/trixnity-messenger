package de.connect2x.trixnity.messenger.viewmodel.connecting

import io.ktor.http.*

sealed interface ServerUrlValidation {
    object None : ServerUrlValidation

    object Started : ServerUrlValidation

    data class Valid(val url: Url) : ServerUrlValidation

    data class Invalid(val message: String) : ServerUrlValidation
}
