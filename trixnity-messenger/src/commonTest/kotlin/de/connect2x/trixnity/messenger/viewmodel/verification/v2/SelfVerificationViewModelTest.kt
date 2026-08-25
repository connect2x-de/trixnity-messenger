package de.connect2x.trixnity.messenger.viewmodel.verification.v2

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.key.KeySecretService
import de.connect2x.trixnity.client.key.KeyService
import de.connect2x.trixnity.client.key.KeyTrustService
import de.connect2x.trixnity.client.verification.ActiveDeviceVerification
import de.connect2x.trixnity.client.verification.ActiveVerificationState
import de.connect2x.trixnity.client.verification.SelfVerificationMethod
import de.connect2x.trixnity.client.verification.VerificationService
import de.connect2x.trixnity.client.verification.VerificationService.SelfVerificationMethods.CrossSigningEnabled
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent
import de.connect2x.trixnity.crypto.key.DeviceTrustLevel
import de.connect2x.trixnity.crypto.key.RecoveryKeyInvalidException
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.verification.VerifyAccount
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SelfVerificationViewModelTest {
    private val userId = UserId("test", "server")

    val matrixClientMock = mock<MatrixClient>()
    val verificationServiceMock = mock<VerificationService>()
    val keyServiceMock = mock<KeyService>()
    val keySecretService = mock<KeySecretService>()
    val keyTrustService = mock<KeyTrustService>()
    val verifyAccountMock = mock<VerifyAccount>()

    val aesHmacSha2Key: SecretKeyEventContent.AesHmacSha2Key = SecretKeyEventContent.AesHmacSha2Key()

    private val onCloseSelfVerificationMock = mock<Function0<Unit>>()
    private val onResetRecoveryMock = mock<Function0<Unit>>()

    init {
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { verificationServiceMock }
                            single { keyServiceMock }
                        }
                    )
                }
                .koin

        every { matrixClientMock.userId } returns userId
        every { matrixClientMock.deviceId } returns "DEVICE"
        every { verificationServiceMock.getSelfVerificationMethods() } returns
            MutableStateFlow(
                CrossSigningEnabled(
                    setOf(
                        SelfVerificationMethod.CrossSignedDeviceVerification(UserId(""), emptySet()) { _, _ ->
                            Result.failure(RuntimeException())
                        }
                    )
                )
            )
        every { keyServiceMock.getTrustLevel(userId, any()) } returns
            MutableStateFlow(DeviceTrustLevel.Valid(verified = true))
        every { onCloseSelfVerificationMock.invoke() } returns Unit
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `does not close the verification if verification state is not determined yet`() = runTest {
        every { keyServiceMock.getTrustLevel(userId, any()) } returns
            flow {
                delay(300.milliseconds)
                emit(DeviceTrustLevel.Valid(verified = true))
            }
        val cut = selfVerificationViewModel()
        delay(100.milliseconds)
        verify(exactly(0)) { onCloseSelfVerificationMock() }
        delay(230.milliseconds)
        verify { onCloseSelfVerificationMock() }
    }

    @Test
    fun `close the device verification if device is already trusted`() = runTest {
        every { keyServiceMock.getTrustLevel(userId, any()) } returns
            MutableStateFlow(DeviceTrustLevel.Valid(verified = true))
        val cut = selfVerificationViewModel()
        delay(100.milliseconds)
        verify { onCloseSelfVerificationMock() }
    }

    @Test
    fun `do not close device verification if device is not trusted yet`() = runTest {
        every { keyServiceMock.getTrustLevel(userId, any()) } returns MutableStateFlow(DeviceTrustLevel.NotCrossSigned)
        val cut = selfVerificationViewModel()
        delay(100.milliseconds)
        verify(exactly(0)) { onCloseSelfVerificationMock() }
    }

    @Test
    fun `availableSelfVerificationMethods should be null if cannot determine whether cross-signing is needed`() =
        runTest {
            every { verificationServiceMock.getSelfVerificationMethods() } returns
                MutableStateFlow(VerificationService.SelfVerificationMethods.PreconditionsNotMet(reasons = setOf()))
            val cut = selfVerificationViewModel()
            delay(100.milliseconds)
            cut.availableSelfVerificationMethods.value shouldBe null
        }

    @Test
    fun `availableSelfVerificationMethods should be empty if cross-signing is disabled`() = runTest {
        every { verificationServiceMock.getSelfVerificationMethods() } returns
            MutableStateFlow(VerificationService.SelfVerificationMethods.NoCrossSigningEnabled)
        val cut = selfVerificationViewModel()
        delay(100.milliseconds)
        cut.availableSelfVerificationMethods.value shouldBe emptyList()
    }

    @Test
    fun `availableSelfVerificationMethods should be empty if already cross-signed`() = runTest {
        every { verificationServiceMock.getSelfVerificationMethods() } returns
            MutableStateFlow(VerificationService.SelfVerificationMethods.AlreadyCrossSigned)
        val cut = selfVerificationViewModel()
        delay(100.milliseconds)
        cut.availableSelfVerificationMethods.value shouldBe emptyList()
    }

    @Test
    fun `availableSelfVerificationMethods should return the available methods if not yet cross-signed`() = runTest {
        every { verificationServiceMock.getSelfVerificationMethods() } returns
            MutableStateFlow(
                CrossSigningEnabled(
                    methods =
                        setOf(
                            SelfVerificationMethod.AesHmacSha2RecoveryKey(
                                keySecretService,
                                keyTrustService,
                                "",
                                aesHmacSha2Key,
                            ),
                            SelfVerificationMethod.CrossSignedDeviceVerification(userId, emptySet()) { _, _ ->
                                Result.failure(RuntimeException())
                            },
                        )
                )
            )
        val cut = selfVerificationViewModel()
        delay(100.milliseconds)
        cut.availableSelfVerificationMethods.value shouldNotBeNull
            {
                map { it::class } shouldBe
                    listOf(
                        SelfVerificationMethod.AesHmacSha2RecoveryKey::class,
                        SelfVerificationMethod.CrossSignedDeviceVerification::class,
                    )
            }
    }

    @Test
    fun `changing the recovery key should reset errors`() = runTest {
        every { verificationServiceMock.getSelfVerificationMethods() } returns
            MutableStateFlow(
                CrossSigningEnabled(
                    methods =
                        setOf(
                            SelfVerificationMethod.AesHmacSha2RecoveryKey(
                                keySecretService,
                                keyTrustService,
                                "",
                                aesHmacSha2Key,
                            ),
                            SelfVerificationMethod.CrossSignedDeviceVerification(userId, emptySet()) { _, _ ->
                                Result.failure(RecoveryKeyInvalidException(""))
                            },
                        )
                )
            )
        everySuspend { verifyAccountMock.verify(any(), any()) } returns Result.failure(RecoveryKeyInvalidException(""))
        val cut = selfVerificationViewModel()
        cut.recoveryKey.update("WRONG")
        delay(100.milliseconds)
        cut.verifyWithRecoveryKey()
        delay(100.milliseconds)
        cut.recoveryKeyWrong.value shouldBe true

        cut.recoveryKey.update("WRONG BUT CHANGED")
        delay(100.milliseconds)
        cut.recoveryKeyWrong.value shouldBe false
    }

    @Test
    fun `changing the passphrase should reset errors`() = runTest {
        every { verificationServiceMock.getSelfVerificationMethods() } returns
            MutableStateFlow(
                CrossSigningEnabled(
                    methods =
                        setOf(
                            SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase(
                                keySecretService,
                                keyTrustService,
                                "",
                                aesHmacSha2Key,
                            ),
                            SelfVerificationMethod.CrossSignedDeviceVerification(userId, emptySet()) { _, _ ->
                                Result.failure(RecoveryKeyInvalidException(""))
                            },
                        )
                )
            )
        everySuspend { verifyAccountMock.verify(any(), any()) } returns Result.failure(RecoveryKeyInvalidException(""))
        val cut = selfVerificationViewModel()
        cut.passphrase.update("WRONG")
        delay(100.milliseconds)
        cut.verifyWithPassphrase()
        delay(100.milliseconds)
        cut.passphraseWrong.value shouldBe true

        cut.passphrase.update("WRONG BUT CHANGED")
        delay(100.milliseconds)
        cut.passphraseWrong.value shouldBe false
    }

    @Test
    fun `verifyWithRecoveryKey should close the self verification if successful`() = runTest {
        every { verificationServiceMock.getSelfVerificationMethods() } returns
            MutableStateFlow(
                CrossSigningEnabled(
                    methods =
                        setOf(
                            SelfVerificationMethod.AesHmacSha2RecoveryKey(
                                keySecretService,
                                keyTrustService,
                                "",
                                aesHmacSha2Key,
                            )
                        )
                )
            )
        everySuspend { verifyAccountMock.verify(any(), any()) } returns Result.success(Unit)
        val cut = selfVerificationViewModel()
        cut.recoveryKey.update("RIGHT")
        delay(100.milliseconds)
        cut.verifyWithRecoveryKey()
        delay(100.milliseconds)
        verify { onCloseSelfVerificationMock.invoke() }
    }

    @Test
    fun `verifyWithPassphrase should close the self verification if successful`() = runTest {
        every { verificationServiceMock.getSelfVerificationMethods() } returns
            MutableStateFlow(
                CrossSigningEnabled(
                    methods =
                        setOf(
                            SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase(
                                keySecretService,
                                keyTrustService,
                                "",
                                aesHmacSha2Key,
                            )
                        )
                )
            )
        everySuspend { verifyAccountMock.verify(any(), any()) } returns Result.success(Unit)
        val cut = selfVerificationViewModel()
        cut.passphrase.update("RIGHT")
        delay(100.milliseconds)
        cut.verifyWithPassphrase()
        delay(100.milliseconds)
        verify { onCloseSelfVerificationMock.invoke() }
    }

    @Test
    fun `verifyWithOtherDevice should trigger device verification and close`() = runTest {
        var createCalled = false
        every { verificationServiceMock.getSelfVerificationMethods() } returns
            MutableStateFlow(
                CrossSigningEnabled(
                    methods =
                        setOf(
                            SelfVerificationMethod.CrossSignedDeviceVerification(userId, emptySet()) { _, _ ->
                                createCalled = true
                                Result.success(
                                    object : ActiveDeviceVerification {
                                        override val theirUserId: UserId = UserId("@their:id")
                                        override val timestamp: Long = 0L
                                        override val relatesTo: RelatesTo.Reference? = null
                                        override val transactionId: String? = null
                                        override val state: StateFlow<ActiveVerificationState> =
                                            MutableStateFlow(ActiveVerificationState.Done)
                                        override val theirDeviceId: String? = null

                                        override suspend fun cancel(message: String) {}
                                    }
                                )
                            }
                        )
                )
            )
        val cut = selfVerificationViewModel()
        delay(100.milliseconds)
        cut.verifyWithOtherDevice()

        delay(100.milliseconds)
        createCalled shouldBe true
        verify { onCloseSelfVerificationMock.invoke() }
    }

    @Test
    fun `reset recovery should trigger reset recovery callback`() = runTest {
        every { onResetRecoveryMock.invoke() } returns Unit
        val cut = selfVerificationViewModel()
        cut.resetRecovery()

        verify { onResetRecoveryMock.invoke() }
    }

    private fun TestScope.selfVerificationViewModel(): SelfVerificationViewModelImpl {
        val viewModel =
            SelfVerificationViewModelImpl(
                viewModelContext =
                    testMatrixClientViewModelContext(
                        di =
                            koinApplication {
                                    modules(
                                        createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClientMock)) +
                                            module { single { verifyAccountMock } }
                                    )
                                }
                                .koin,
                        userId = userId,
                    ),
                onCloseSelfVerification = onCloseSelfVerificationMock,
                onResetRecovery = onResetRecoveryMock,
            )
        viewModel.availableSelfVerificationMethods.launchIn(backgroundScope)
        return viewModel
    }
}
