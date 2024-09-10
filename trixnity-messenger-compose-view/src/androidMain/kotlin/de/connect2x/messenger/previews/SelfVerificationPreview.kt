package de.connect2x.messenger.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.verification.RedoSelfVerificationModal
import de.connect2x.messenger.compose.view.verification.SelfVerificationModal
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.verification.RedoSelfVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.client.key.KeySecretService
import net.folivo.trixnity.client.key.KeyTrustService
import net.folivo.trixnity.client.store.KeySignatureTrustLevel
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import net.folivo.trixnity.client.verification.SelfVerificationMethod.AesHmacSha2RecoveryKey
import net.folivo.trixnity.client.verification.SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase
import net.folivo.trixnity.client.verification.SelfVerificationMethod.CrossSignedDeviceVerification
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.SignedCrossSigningKeys
import net.folivo.trixnity.core.model.keys.SignedDeviceKeys


@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun SelfVerificationPreview() {
    InitMessengerPreview {
        SelfVerificationModal(SelfVerificationViewModelPreview())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun SelfVerificationPreviewWithRecoveryKey() {
    InitMessengerPreview {
        SelfVerificationModal(
            SelfVerificationViewModelPreview(
                aesHmacSha2RecoveryKey = aesHmacSha2RecoveryKey(),
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RedoSelfVerificationPreview() {
    InitMessengerPreview {
        RedoSelfVerificationModal(RedoSelfVerificationViewModelPreview())
    }
}


private val demoUserId = UserId("@demo:timmy-messenger.de")

private fun aesHmacSha2RecoveryKey() = AesHmacSha2RecoveryKey(
    keyTrustService = object : KeyTrustService {
        override suspend fun calculateCrossSigningKeysTrustLevel(crossSigningKeys: SignedCrossSigningKeys) =
            KeySignatureTrustLevel.NotCrossSigned

        override suspend fun calculateDeviceKeysTrustLevel(deviceKeys: SignedDeviceKeys) =
            KeySignatureTrustLevel.NotCrossSigned

        override suspend fun checkOwnAdvertisedMasterKeyAndVerifySelf(
            key: ByteArray,
            keyId: String,
            keyInfo: SecretKeyEventContent
        ) = Result.success(Unit)

        override suspend fun trustAndSignKeys(keys: Set<Key.Ed25519Key>, userId: UserId) {}
        override suspend fun updateTrustLevelOfKeyChainSignedBy(signingUserId: UserId, signingKey: Key.Ed25519Key) {}
    },
    keyId = "1",
    info = SecretKeyEventContent.AesHmacSha2Key(null, null, null, null),
    keySecretService = object : KeySecretService {
        override suspend fun decryptMissingSecrets(key: ByteArray, keyId: String, keyInfo: SecretKeyEventContent) {}
    }
)

// TODO: move into Trixnity Messenger
private class SelfVerificationViewModelPreview(
    aesHmacSha2RecoveryKey: AesHmacSha2RecoveryKey? = null,
    aesHmacSha2RecoveryKeyWithPbkdf2Passphrase: AesHmacSha2RecoveryKeyWithPbkdf2Passphrase? = null,
    crossSignedDeviceVerification: CrossSignedDeviceVerification? = null,
) : SelfVerificationViewModel {
    override val error: MutableStateFlow<String?> = MutableStateFlow("")
    override val passphraseWrong: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val recoveryKeyWrong: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val selfVerificationMethods: MutableStateFlow<Set<SelfVerificationMethod>> = MutableStateFlow(
        setOf(
            aesHmacSha2RecoveryKey,
            aesHmacSha2RecoveryKeyWithPbkdf2Passphrase,
            crossSignedDeviceVerification,
        ).filterNotNull().toSet()
    )
    override val showPassphraseMethod = MutableStateFlow(aesHmacSha2RecoveryKeyWithPbkdf2Passphrase)
    override val showRecoveryKeyMethod = MutableStateFlow(aesHmacSha2RecoveryKey)
    override val showVerificationHelp: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val userId = demoUserId
    override fun backToChoose() {}
    override fun close() {}
    override fun closeMessenger() {}
    override fun launchVerification(selfVerificationMethod: SelfVerificationMethod) {}
    override fun verifyWithPassphrase(passphrase: String) {}
    override fun verifyWithRecoveryKey(recoveryKey: String) {}
    override fun waitForAvailableVerificationMethods() {}
}

// TODO: move into Trixnity Messenger
private class RedoSelfVerificationViewModelPreview : RedoSelfVerificationViewModel {
    override val userId = demoUserId
    override fun close() {}
    override fun startSelfVerification() {}
}
