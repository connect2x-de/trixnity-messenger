package de.connect2x.livekitnity.api.e2ee

public data class E2EEOptions(
    val keyProviderOptions: KeyProviderOptions = KeyProviderOptions(),
    val encryptionType: EncryptionType = EncryptionType.GCM,
)
