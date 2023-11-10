package de.connect2x.trixnity.messenger.util

interface Secrets { // for mocking
    suspend fun getSecret(id: String): String? {
        return de.connect2x.trixnity.messenger.util.getSecret(id)
    }

    suspend fun setSecret(id: String, secret: String) {
        de.connect2x.trixnity.messenger.util.setSecret(id, secret)
    }

    companion object : Secrets
}

expect suspend fun getSecret(id: String): String?

expect suspend fun setSecret(id: String, secret: String)