package de.connect2x.trixnity.messenger.secrets

import org.koin.dsl.module

fun secretsModule() = module {
    includes(platformSecretByteArrayKeyProviderModule())
    single<SecretByteArrays> { SecretByteArraysImpl(get(), getKoin()) }
}
