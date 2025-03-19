package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.KeySecretService
import net.folivo.trixnity.client.key.KeyTrustService
import net.folivo.trixnity.client.verification.ActiveDeviceVerification
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.client.verification.VerificationService.SelfVerificationMethods.CrossSigningEnabled
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent
import net.folivo.trixnity.crypto.key.RecoveryKeyInvalidException
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

class SelfVerificationViewModelTest {
    val matrixClientMock = mock<MatrixClient>()

    val verificationServiceMock = mock<VerificationService>()

    val keySecretService = mock<KeySecretService>()

    val keyTrustService = mock<KeyTrustService>()

    val verifyAccountMock = mock<VerifyAccount>()

    val aesHmacSha2Key: SecretKeyEventContent.AesHmacSha2Key = SecretKeyEventContent.AesHmacSha2Key()

    private val onCloseSelfVerificationMock = mock<Function1<Boolean, Unit>>()

    private val onResetRecoveryMock = mock<Function0<Unit>>()

    init {
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { verificationServiceMock }
                })
        }.koin

        every { matrixClientMock.userId } returns UserId("test", "localhost")
        every { verificationServiceMock.getSelfVerificationMethods() } returns MutableStateFlow(
            CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                        Result.failure(
                            RuntimeException()
                        )
                    },
                )
            )
        )
    }

    @Test
    fun `show verification help, initially`() = runTest {
        val cut = selfVerificationViewModel()

        cut.showVerificationHelp.value shouldBe true
        cut.showPassphraseMethod.value shouldBe null
        cut.showRecoveryKeyMethod.value shouldBe null
        cut.recoveryKeyWrong.value shouldBe false
        cut.error.value shouldBe null
    }

    @Test
    fun `show reset recovery warning`() = runTest {
        val cut = selfVerificationViewModel()
        cut.resetRecoveryWarning()

        cut.showVerificationHelp.value shouldBe false
        cut.showResetRecoveryWarning.value shouldBe true
    }

    @Test
    fun `reset recovery`() = runTest {
        every { onResetRecoveryMock.invoke() } returns Unit
        val cut = selfVerificationViewModel()
        cut.resetRecovery()

        verify { onResetRecoveryMock.invoke() }
    }

    @Test
    fun `show self verification options (even if there is only one)`() = runTest {
        val selfVerificationMethods = MutableStateFlow(
            CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                        Result.failure(
                            RuntimeException()
                        )
                    },
                )
            )
        )
        every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

        val cut = selfVerificationViewModel()
        cut.waitForAvailableVerificationMethods()
        delay(100)

        cut.showVerificationHelp.value shouldBe false
        cut.selfVerificationMethods.value shouldBe selfVerificationMethods.value.methods
        cut.showPassphraseMethod.value shouldBe null
        cut.showRecoveryKeyMethod.value shouldBe null
    }

    @Test
    fun `close the self verification modal after choosing device verification`() = runTest {
        var deviceVerificationCalled = false
        val createDeviceVerification: suspend (UserId, Set<String>) -> Result<ActiveDeviceVerification> =
            { _: UserId, _: Set<String> ->
                deviceVerificationCalled = true
                Result.failure(RuntimeException())
            }

        val deviceVerification =
            SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), setOf(), createDeviceVerification)
        val selfVerificationMethods = MutableStateFlow(
            CrossSigningEnabled(
                setOf(
                    deviceVerification,
                    SelfVerificationMethod.AesHmacSha2RecoveryKey(
                        keySecretService, keyTrustService, "", aesHmacSha2Key
                    ),
                )
            )
        )
        every { onCloseSelfVerificationMock.invoke(any()) } returns Unit
        every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

        val cut = selfVerificationViewModel()
        cut.waitForAvailableVerificationMethods()
        cut.launchVerification(deviceVerification)
        delay(100)

        deviceVerificationCalled shouldBe true
        verify { onCloseSelfVerificationMock.invoke(any()) }
    }

    @Test
    fun `show the recovery key UI after choosing the recovery key method`() = runTest {
        val recoveryKeyMethod =
            SelfVerificationMethod.AesHmacSha2RecoveryKey(keySecretService, keyTrustService, "", aesHmacSha2Key)
        val selfVerificationMethods = MutableStateFlow(
            CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                        Result.failure(RuntimeException())
                    },
                    recoveryKeyMethod,
                )
            )
        )
        every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

        val cut = selfVerificationViewModel()
        cut.waitForAvailableVerificationMethods()
        cut.launchVerification(recoveryKeyMethod)

        cut.showRecoveryKeyMethod.value shouldBe recoveryKeyMethod
        cut.showPassphraseMethod.value shouldBe null
    }

    @Test
    fun `show the passphrase UI after choosing the passphrase method`() = runTest {
        val passphraseMethod = SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase(
            keySecretService, keyTrustService, "", aesHmacSha2Key
        )
        val selfVerificationMethods = MutableStateFlow(
            CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                        Result.failure(RuntimeException())
                    },
                    passphraseMethod,
                )
            )
        )
        every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

        val cut = selfVerificationViewModel()
        cut.waitForAvailableVerificationMethods()
        cut.launchVerification(passphraseMethod)

        cut.showPassphraseMethod.value shouldBe passphraseMethod
        cut.showRecoveryKeyMethod.value shouldBe null
    }

    @Test
    fun `close the modal when the recovery key is correct`() = runTest {
        val recoveryKeyMethod =
            SelfVerificationMethod.AesHmacSha2RecoveryKey(keySecretService, keyTrustService, "", aesHmacSha2Key)
        val selfVerificationMethods = MutableStateFlow(
            CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                        Result.failure(RuntimeException())
                    }, recoveryKeyMethod
                )
            )
        )
        every { onCloseSelfVerificationMock.invoke(any()) } returns Unit
        everySuspend {
            verifyAccountMock.verify(any(), eq("iAmA Reco very Key1"))
        } returns Result.success(Unit)
        every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

        val cut = selfVerificationViewModel()
        cut.waitForAvailableVerificationMethods()
        cut.launchVerification(recoveryKeyMethod)
        delay(100)

        cut.verifyWithRecoveryKey("iAmA Reco very Key1")
        cut.recoveryKeyWrong.value shouldBe false
        delay(100)

        verifySuspend {
            verifyAccountMock.verify(any(), eq("iAmA Reco very Key1"))
            onCloseSelfVerificationMock.invoke(any())
        }
    }

    @Test
    fun `indicate when the recovery key is not correct`() = runTest {
        val recoveryKeyMethod =
            SelfVerificationMethod.AesHmacSha2RecoveryKey(keySecretService, keyTrustService, "", aesHmacSha2Key)
        val selfVerificationMethods = MutableStateFlow(
            CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                        Result.failure(RuntimeException())
                    }, recoveryKeyMethod
                )
            )
        )
        var onCloseMockWasCalled = false
        every { onCloseSelfVerificationMock.invoke(any()) } calls { onCloseMockWasCalled = true }
        everySuspend { verifyAccountMock.verify(any(), eq("iAmA Sooo very Wron")) } returns Result.failure(
            RecoveryKeyInvalidException("Nope")
        )
        every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

        val cut = selfVerificationViewModel()
        cut.waitForAvailableVerificationMethods()
        cut.launchVerification(recoveryKeyMethod)
        delay(100)
        cut.verifyWithRecoveryKey("iAmA Sooo very Wron")

        cut.recoveryKeyWrong.value shouldBe false
        delay(100)

        onCloseMockWasCalled shouldBe false
    }

    @Test
    fun `display an error message when the verification of the recovery key throws an unexpected error`() = runTest {
        val recoveryKeyMethod =
            SelfVerificationMethod.AesHmacSha2RecoveryKey(keySecretService, keyTrustService, "", aesHmacSha2Key)
        val selfVerificationMethods = MutableStateFlow(
            CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                        Result.failure(RuntimeException())
                    }, recoveryKeyMethod
                )
            )
        )
        var onCloseMockWasCalled = false
        every { onCloseSelfVerificationMock.invoke(any()) } calls { onCloseMockWasCalled = true }
        everySuspend { verifyAccountMock.verify(any(), eq("iAmA Reco very Key1")) } returns Result.failure(
            RuntimeException("Oh no!")
        )
        every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

        val cut = selfVerificationViewModel()
        cut.waitForAvailableVerificationMethods()
        cut.launchVerification(recoveryKeyMethod)
        delay(100)

        cut.verifyWithRecoveryKey("iAmA Reco very Key1")
        cut.recoveryKeyWrong.value shouldBe false

        delay(100)

        cut.error.value shouldNotBe null
        onCloseMockWasCalled shouldBe false
    }

    @Test
    fun `close any chosen verification method when going back to choose a verification method`() = runTest {
        val recoveryKey =
            SelfVerificationMethod.AesHmacSha2RecoveryKey(keySecretService, keyTrustService, "", aesHmacSha2Key)
        val selfVerificationMethods = MutableStateFlow(
            CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                        Result.failure(RuntimeException())
                    }, recoveryKey
                )
            )
        )
        every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

        val cut = selfVerificationViewModel()
        cut.waitForAvailableVerificationMethods()
        cut.launchVerification(recoveryKey)
        cut.backToChoose()

        cut.showVerificationHelp.first { it.not() }
        cut.showPassphraseMethod.first { it == null }
        cut.showRecoveryKeyMethod.first { it == null }
        cut.selfVerificationMethods.first { it == selfVerificationMethods.value.methods }
    }

    private fun TestScope.selfVerificationViewModel(): SelfVerificationViewModelImpl {
        return SelfVerificationViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(
                                UserId(
                                    "test", "server"
                                ) to matrixClientMock
                            )
                        ) + module {
                            single { verifyAccountMock }
                        })
                }.koin,
                userId = UserId("test", "server"),
            ),
            onCloseSelfVerification = onCloseSelfVerificationMock,
            onResetRecovery = onResetRecoveryMock,
        )
    }

}
