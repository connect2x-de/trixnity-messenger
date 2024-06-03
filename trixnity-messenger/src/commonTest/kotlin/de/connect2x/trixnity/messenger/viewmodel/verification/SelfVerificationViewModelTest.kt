package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
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
import org.kodein.mock.Fake
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class SelfVerificationViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var verificationServiceMock: VerificationService

    @Mock
    lateinit var keySecretService: KeySecretService

    @Mock
    lateinit var keyTrustService: KeyTrustService

    @Mock
    lateinit var verifyAccountMock: VerifyAccount

    @Fake
    lateinit var aesHmacSha2Key: SecretKeyEventContent.AesHmacSha2Key

    private val onCloseSelfVerificationMock = mockFunction0<Unit>(mocker)

    private val onCloseSelfVerificationViewMock = mockFunction0<Unit>(mocker)

    init {
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { verificationServiceMock }
                        }
                    )
                }.koin

                every { matrixClientMock.userId } returns UserId("test", "localhost")
            }
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
            mocker.every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

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
            mocker.every { onCloseSelfVerificationViewMock.invoke() } returns Unit
            mocker.every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

            val cut = selfVerificationViewModel(coroutineContext)
            cut.waitForAvailableVerificationMethods()
            cut.launchVerification(deviceVerification)
            testCoroutineScheduler.advanceUntilIdle()

            deviceVerificationCalled shouldBe true
            mocker.verify(exhaustive = false) { onCloseSelfVerificationViewMock.invoke() }

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
            mocker.every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

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
            mocker.every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

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
            with(mocker) {
                every { onCloseSelfVerificationMock.invoke() } returns Unit
                everySuspending {
                    verifyAccountMock.verify(isAny(), isEqual("iAmA Reco very Key1"))
                } returns Result.success(Unit)
                every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods
            }

            val cut = selfVerificationViewModel(coroutineContext)
            cut.waitForAvailableVerificationMethods()
            cut.launchVerification(recoveryKeyMethod)
            testCoroutineScheduler.advanceUntilIdle()
            cut.verifyWithRecoveryKey("iAmA Reco very Key1")

            cut.recoveryKeyWrong.value shouldBe false
            testCoroutineScheduler.advanceUntilIdle()
            mocker.verifyWithSuspend(exhaustive = false) {
                verifyAccountMock.verify(isAny(), isEqual("iAmA Reco very Key1"))
                onCloseSelfVerificationMock.invoke()
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
            with(mocker) {
                every { onCloseSelfVerificationMock.invoke() } runs { onCloseMockWasCalled = true }
                everySuspending { verifyAccountMock.verify(isAny(), isEqual("iAmA Sooo very Wron")) } returns
                        Result.failure(RecoveryKeyInvalidException("Nope"))
                every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods
            }

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
            with(mocker) {
                every { onCloseSelfVerificationMock.invoke() } runs { onCloseMockWasCalled = true }
                everySuspending { verifyAccountMock.verify(isAny(), isEqual("iAmA Reco very Key1")) } returns
                        Result.failure(RuntimeException("Oh no!"))
                every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods
            }

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
            mocker.every { verificationServiceMock.getSelfVerificationMethods() } returns selfVerificationMethods

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
            onCloseSelfVerificationView = onCloseSelfVerificationViewMock,
            onCloseSelfVerification = onCloseSelfVerificationMock,
        )
    }

}
