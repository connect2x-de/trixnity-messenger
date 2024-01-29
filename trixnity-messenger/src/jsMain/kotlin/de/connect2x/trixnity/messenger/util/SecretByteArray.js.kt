package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformConvertSecretByteArray(): Module = module {
    single<ConvertSecretByteArray> {
        object : ConvertSecretByteArray {
            override suspend operator fun invoke(raw: ByteArray): SecretByteArray = SecretByteArray.Unencrypted(raw)

            override suspend operator fun invoke(secret: SecretByteArray): ByteArray = when (secret) {
                is SecretByteArray.Unencrypted -> secret.value
                is SecretByteArray.AesHmacSha2 -> throw IllegalArgumentException("encrypted StringSecrets not supported in JavaScript")
            }
        }
    }
}