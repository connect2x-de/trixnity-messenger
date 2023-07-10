package de.connect2x.trixnity.messenger.util

interface Secrets { // for mocking
    fun getSecret(id: String): String? {
        return de.connect2x.trixnity.messenger.util.getSecret(id)
    }

    fun setSecret(id: String, secret: String) {
        de.connect2x.trixnity.messenger.util.setSecret(id, secret)
    }
}

expect fun getSecret(id: String): String?

expect fun setSecret(id: String, secret: String)