package de.connect2x.trixnity.messenger.util

import kotlinx.browser.localStorage

actual suspend fun getSecret(id: String): String? {
    return localStorage.getItem(id) // there is no real secure way to store secrets
}

actual suspend fun setSecret(id: String, secret: String) {
    localStorage.setItem(id, secret)
}