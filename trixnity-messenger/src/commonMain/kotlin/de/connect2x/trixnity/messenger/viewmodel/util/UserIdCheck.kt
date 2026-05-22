package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.core.model.UserId
import io.ktor.http.*

fun UserId.isValid(): Boolean {
    val hasDomain = full.contains(":")
    val isUrlOk =
        try {
            Url(this.domain)
            true
        } catch (exc: Exception) {
            false
        }
    val doesStartWithAt = full.startsWith("@")
    return hasDomain && isUrlOk && doesStartWithAt
}
