package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import org.koin.core.module.Module
import org.koin.dsl.module

const val SECRET_BYTE_ARRAY_KEY_KEY =
    "secret_byte_array_key_key" // FIXME seems to be exposed, when mac UI asks to access this key

fun interface GetPlatformSecret {
    suspend operator fun invoke(id: String, sizeOnCreate: Int): ByteArray?
}

expect fun platformGetPlatformSecret(): Module
actual fun platformGetSecretByteArrayKey(): Module = module {
    includes(platformGetPlatformSecret())
    single<GetSecretByteArrayKey> {
        val settings = get<MatrixMessengerSettingsHolder>()
        val getPlatformSecret = get<GetPlatformSecret>()
        object : GetSecretByteArrayKeyBase(settings) {
            override suspend fun getSecretByteArrayKeyKey(sizeOnCreate: Int) =
                getPlatformSecret(SECRET_BYTE_ARRAY_KEY_KEY, sizeOnCreate)
        }
    }
}