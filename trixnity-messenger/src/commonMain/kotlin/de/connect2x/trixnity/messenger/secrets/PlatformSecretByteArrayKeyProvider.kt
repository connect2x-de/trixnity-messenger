package de.connect2x.trixnity.messenger.secrets

import org.koin.core.module.Module

internal const val PLATFORM_SECRET_BYTE_ARRAY_KEY_PROVIDER_ID = "de.connect2x.trixnity.messenger.secrets.platform"
expect fun platformSecretByteArrayKeyProviderModule(): Module
