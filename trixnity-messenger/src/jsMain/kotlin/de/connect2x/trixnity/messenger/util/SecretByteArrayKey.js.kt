package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformGetSecretByteArrayKey(): Module = module {
    single<GetSecretByteArrayKey> {
        val settings = get<MatrixMessengerSettingsHolder>()
        object : GetSecretByteArrayKeyBase(settings) {
            override suspend fun getSecretByteArrayKeyKey(sizeOnCreate: Int): ByteArray? = null
        }
    }
}