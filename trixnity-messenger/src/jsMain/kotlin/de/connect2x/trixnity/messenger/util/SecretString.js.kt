package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformConvertSecretString(): Module = module {
    single<ConvertSecretString> {
        object : ConvertSecretString {
            override suspend fun get(raw: String): SecretString = SecretString.Unencrypted(raw)

            override suspend fun get(secret: SecretString): String = when (secret) {
                is SecretString.Unencrypted -> secret.value
                is SecretString.AesHmacSha2 -> throw IllegalArgumentException("encrypted StringSecrets not supported in JavaScript")
            }
        }
    }
}