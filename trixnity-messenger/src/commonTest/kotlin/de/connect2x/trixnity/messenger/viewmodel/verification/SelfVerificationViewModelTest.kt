package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
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
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
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
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class SelfVerificationViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val matrixClientMock = mock<MatrixClient>()

    val verificationServiceMock = mock<VerificationService>()

    val keySecretService = mock<KeySecretService>()

    val keyTrustService = mock<KeyTrustService>()

    val verifyAccountMock = mock<VerifyAccount>()

    val aesHmacSha2Key: SecretKeyEventContent.AesHmacSha2Key = SecretKeyEventContent.AesHmacSha2Key()

    private val onCloseSelfVerificationMock = mock<Function1<Boolean, Unit>>()

    private val onResetRecoveryMock = mock<Function0<Unit>>()

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(
                matrixClientMock,
                verificationServiceMock,
                keySecretService,
                keyTrustService,
                verifyAccountMock,
                onCloseSelfVerificationMock,
                onResetRecoveryMock
            )
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { verificationServiceMock }
                    }
                )
            }.koin

            every { matrixClientMock.userId } returns UserId("test", "localhost")
        }

        should("show verification help, initially") {
            val cut = selfVerificationViewModel(coroutineContext)

            cut.showVerificationHelp.value shouldBe true
            cut.showPassphraseMethod.value shouldBe null
            cut.showRecoveryKeyMethod.value shouldBe null
            cut.recoveryKeyWrong.value shouldBe false
            cut.error.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("show reset recovery warning") {
            val cut = selfVerificationViewModel(coroutineContext)
            cut.resetRecoveryWarning()
            testCoroutineScheduler.advanceUntilIdle()

            cut.showVerificationHelp.value shouldBe false
            cut.showResetRecoveryWarning.value shouldBe true

            cancelNeverEndingCoroutines()
        }

        should("reset recovery") {
            every { onResetRecoveryMock.invoke() } returns Unit
            val cut = selfVerificationViewModel(coroutineContext)
            cut.resetRecovery()
            testCoroutineScheduler.advanceUntilIdle()

            verify { onResetRecoveryMock.invoke() }

            cancelNeverEndingCoroutines()
        }

        should("show self verification options (even if there is only one)") {
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

            val cut = selfVerificationViewModel(coroutineContext)
            cut.waitForAvailableVerificationMethods()
            testCoroutineScheduler.advanceUntilIdle()

            cut.showVerificationHelp.value shouldBe false
            cut.selfVerificationMethods.value shouldBe selfVerificationMethods.value.methods
            cut.showPassphraseMethod.value shouldBe null
            cut.showRecoveryKeyMethod.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("close the self verification modal after choosing device verification") {
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
                            keySecretService,
                            keyTrustService,
                            "",
                            aesHmacSha2Key
                        ),
                    )
                )
            )
            every { onCloseSelfVerificationMock.invoke(any()) } returns Unit
            every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

            val cut = selfVerificationViewModel(coroutineContext)
            cut.waitForAvailableVerificationMethods()
            cut.launchVerification(deviceVerification)
            testCoroutineScheduler.advanceUntilIdle()

            deviceVerificationCalled shouldBe true
            verify { onCloseSelfVerificationMock.invoke(any()) }

            cancelNeverEndingCoroutines()
        }

        should("show the recovery key UI after choosing the recovery key method") {
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

            val cut = selfVerificationViewModel(coroutineContext)
            cut.waitForAvailableVerificationMethods()
            cut.launchVerification(recoveryKeyMethod)
            testCoroutineScheduler.advanceUntilIdle()

            cut.showRecoveryKeyMethod.value shouldBe recoveryKeyMethod
            cut.showPassphraseMethod.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("show the passphrase UI after choosing the passphrase method") {
            val passphraseMethod =
                SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase(
                    keySecretService,
                    keyTrustService,
                    "",
                    aesHmacSha2Key
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

            val cut = selfVerificationViewModel(coroutineContext)
            cut.waitForAvailableVerificationMethods()
            cut.launchVerification(passphraseMethod)
            testCoroutineScheduler.advanceUntilIdle()

            cut.showPassphraseMethod.value shouldBe passphraseMethod
            cut.showRecoveryKeyMethod.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("close the modal when the recovery key is correct") {
            val recoveryKeyMethod =
                SelfVerificationMethod.AesHmacSha2RecoveryKey(keySecretService, keyTrustService, "", aesHmacSha2Key)
            val selfVerificationMethods = MutableStateFlow(
                CrossSigningEnabled(
                    setOf(
                        SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                            Result.failure(RuntimeException())
                        },
                        recoveryKeyMethod
                    )
                )
            )
            every { onCloseSelfVerificationMock.invoke(any()) } returns Unit
            everySuspend {
                verifyAccountMock.verify(any(), eq("iAmA Reco very Key1"))
            } returns Result.success(Unit)
            every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

            val cut = selfVerificationViewModel(coroutineContext)
            cut.waitForAvailableVerificationMethods()
            cut.launchVerification(recoveryKeyMethod)
            testCoroutineScheduler.advanceUntilIdle()
            cut.verifyWithRecoveryKey("iAmA Reco very Key1")

            cut.recoveryKeyWrong.value shouldBe false
            testCoroutineScheduler.advanceUntilIdle()
            verifySuspend {
                verifyAccountMock.verify(any(), eq("iAmA Reco very Key1"))
                onCloseSelfVerificationMock.invoke(any())
            }

            cancelNeverEndingCoroutines()
        }

        should("indicate when the recovery key is not correct") {
            val recoveryKeyMethod =
                SelfVerificationMethod.AesHmacSha2RecoveryKey(keySecretService, keyTrustService, "", aesHmacSha2Key)
            val selfVerificationMethods = MutableStateFlow(
                CrossSigningEnabled(
                    setOf(
                        SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                            Result.failure(RuntimeException())
                        },
                        recoveryKeyMethod
                    )
                )
            )
            var onCloseMockWasCalled = false
            every { onCloseSelfVerificationMock.invoke(any()) } calls { onCloseMockWasCalled = true }
            everySuspend { verifyAccountMock.verify(any(), eq("iAmA Sooo very Wron")) } returns
                    Result.failure(RecoveryKeyInvalidException("Nope"))
            every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

            val cut = selfVerificationViewModel(coroutineContext)
            cut.waitForAvailableVerificationMethods()
            cut.launchVerification(recoveryKeyMethod)
            testCoroutineScheduler.advanceUntilIdle()
            cut.verifyWithRecoveryKey("iAmA Sooo very Wron")

            cut.recoveryKeyWrong.value shouldBe false
            testCoroutineScheduler.advanceUntilIdle()

            onCloseMockWasCalled shouldBe false

            cancelNeverEndingCoroutines()
        }

        should("display an error message when the verification of the recovery key throws an unexpected error") {
            val recoveryKeyMethod =
                SelfVerificationMethod.AesHmacSha2RecoveryKey(keySecretService, keyTrustService, "", aesHmacSha2Key)
            val selfVerificationMethods = MutableStateFlow(
                CrossSigningEnabled(
                    setOf(
                        SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                            Result.failure(RuntimeException())
                        },
                        recoveryKeyMethod
                    )
                )
            )
            var onCloseMockWasCalled = false
            every { onCloseSelfVerificationMock.invoke(any()) } calls { onCloseMockWasCalled = true }
            everySuspend { verifyAccountMock.verify(any(), eq("iAmA Reco very Key1")) } returns
                    Result.failure(RuntimeException("Oh no!"))
            every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

            val cut = selfVerificationViewModel(coroutineContext)
            cut.waitForAvailableVerificationMethods()
            cut.launchVerification(recoveryKeyMethod)
            testCoroutineScheduler.advanceUntilIdle()
            cut.verifyWithRecoveryKey("iAmA Reco very Key1")

            cut.recoveryKeyWrong.value shouldBe false
            testCoroutineScheduler.advanceUntilIdle()

            cut.error.value shouldNotBe null
            onCloseMockWasCalled shouldBe false

            cancelNeverEndingCoroutines()
        }

        should("close any chosen verification method when going back to choose a verification method") {
            val recoveryKey =
                SelfVerificationMethod.AesHmacSha2RecoveryKey(keySecretService, keyTrustService, "", aesHmacSha2Key)
            val selfVerificationMethods = MutableStateFlow(
                CrossSigningEnabled(
                    setOf(
                        SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                            Result.failure(RuntimeException())
                        },
                        recoveryKey
                    )
                )
            )
            every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

            val cut = selfVerificationViewModel(coroutineContext)
            cut.waitForAvailableVerificationMethods()
            cut.launchVerification(recoveryKey)
            cut.backToChoose()
            testCoroutineScheduler.advanceUntilIdle()

            cut.showVerificationHelp.first { it.not() }
            cut.showPassphraseMethod.first { it == null }
            cut.showRecoveryKeyMethod.first { it == null }
            cut.selfVerificationMethods.first { it == selfVerificationMethods.value.methods }

            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun selfVerificationViewModel(coroutineContext: CoroutineContext): SelfVerificationViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return SelfVerificationViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(
                                UserId(
                                    "test",
                                    "server"
                                ) to matrixClientMock
                            )
                        ) + module {
                            single { verifyAccountMock }
                        })
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            onCloseSelfVerification = onCloseSelfVerificationMock,
            onResetRecovery = onResetRecoveryMock,
        )
    }

}
