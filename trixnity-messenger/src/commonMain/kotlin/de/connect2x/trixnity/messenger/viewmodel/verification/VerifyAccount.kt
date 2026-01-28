package de.connect2x.trixnity.messenger.viewmodel.verification


import de.connect2x.trixnity.client.verification.SelfVerificationMethod
import de.connect2x.lognity.api.logger.Logger

// wrapped since we need to mock the behaviour in the tests
interface VerifyAccount {
    suspend fun verify(selfVerificationMethod: SelfVerificationMethod, input: String): Result<Unit>
}

// need a class since mock generates non-suspending function -> compile error
class VerifyAccountImpl : VerifyAccount {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.verification.VerifyAccountImpl")
    }

    override suspend fun verify(selfVerificationMethod: SelfVerificationMethod, input: String): Result<Unit> {
        return when (selfVerificationMethod) {
            is SelfVerificationMethod.AesHmacSha2RecoveryKey -> selfVerificationMethod.verify(input)
            is SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase -> selfVerificationMethod.verify(input)
            else -> {
                log.warn { "Cannot verify $selfVerificationMethod. Is neither recovery key nor passphrase." }
                Result.failure(IllegalArgumentException())
            }
        }
    }
}
