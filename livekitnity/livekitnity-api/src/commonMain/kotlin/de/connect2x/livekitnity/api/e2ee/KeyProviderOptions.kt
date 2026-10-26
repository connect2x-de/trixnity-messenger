package de.connect2x.livekitnity.api.e2ee

public data class KeyProviderOptions(
    val ratchetSalt: String = "LKFrameEncryptionKey",
    val ratchetWindowSize: Int = 16,
    val enableSharedKey: Boolean = true,
    val failureTolerance: Int = -1,
    val keyRingSize: Int = 16,
    val keyDerivationAlgorithm: KeyDerivationAlgorithm = KeyDerivationAlgorithm.PBKDF2,
)
