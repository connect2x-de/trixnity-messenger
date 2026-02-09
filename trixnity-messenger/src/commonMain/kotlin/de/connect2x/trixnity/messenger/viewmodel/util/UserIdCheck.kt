package de.connect2x.trixnity.messenger.viewmodel.util

import io.ktor.http.*
import de.connect2x.trixnity.core.model.UserId

fun UserId.isValid(): Boolean {
    val hasDomain = full.contains(":")
    val isUrlOk = try {
        Url(this.domain)
        true
    } catch (exc: Exception) {
        false
    }
    val doesStartWithAt = full.startsWith("@")
    return hasDomain && isUrlOk && doesStartWithAt
}
