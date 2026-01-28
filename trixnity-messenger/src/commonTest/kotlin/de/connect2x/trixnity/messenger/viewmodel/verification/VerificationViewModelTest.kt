package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.settle
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.verification.ActiveDeviceVerification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.clientserverapi.client.DeviceApiClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.clientserverapi.model.devices.Device
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequestToDeviceEventContent
import net.folivo.trixnity.crypto.key.DeviceTrustLevel
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class VerificationViewModelTest {

    private val ownUserId = UserId("userId", "localhost")
    private val ownDeviceId = "deviceId"
    private val otherUserId = UserId("otherUserId")
    private val otherDeviceId = "otherDevice"

    val matrixClientMock = mock<MatrixClient>()

    val keyServiceMock = mock<KeyService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val devicesApiClientMock = mock<DeviceApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val verificationService = mock<VerificationService>()

    val activeVerification = mock<ActiveDeviceVerification>()

    val activeVerification2 = mock<ActiveDeviceVerification>()

    private val onCloseDeviceVerificationMock = mock<Function0<Unit>>()
    private val onRedoSelfVerificationMock = mock<Function0<Unit>>()
    private val activeDeviceVerificationFlow: MutableStateFlow<ActiveDeviceVerification?> =
        MutableStateFlow(activeVerification)

    init {
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { keyServiceMock }
                    single { verificationService }
                })
        }.koin
        every { matrixClientMock.userId } returns ownUserId
        every { matrixClientMock.deviceId } returns ownDeviceId
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientMock.displayName } returns MutableStateFlow("otherUser")
        every { matrixClientServerApiClientMock.json } returns Json
        every { matrixClientServerApiClientMock.device } returns devicesApiClientMock
        every { matrixClientServerApiClientMock.user } returns usersApiClientMock

        everySuspend {
            devicesApiClientMock.getDevice(any(), null)
        } returns Result.success(Device(ownDeviceId))
        everySuspend { usersApiClientMock.getDisplayName(otherUserId) } returns Result.success("otherUser")

        every { verificationService.activeDeviceVerification } returns activeDeviceVerificationFlow
        every { activeVerification.theirDeviceId } returns otherDeviceId
        every { activeVerification.theirUserId } returns otherUserId
        every { activeVerification2.theirDeviceId } returns otherDeviceId
        every { activeVerification2.theirUserId } returns otherUserId

        every { onCloseDeviceVerificationMock.invoke() } returns Unit
    }


    @Test
    fun `show verification request when a verification is started by another client`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(theirRequest())

        val cut = deviceVerificationViewModel()

        eventually(1.seconds) {
            cut.stack.value.active.configuration should beOfType<Config.Request>()
        }
    }

    @Test
    fun `start verification if verification state is 'ready'`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(
            ActiveVerificationState.Ready(
                ownDeviceId = ownDeviceId,
                methods = emptySet(),
                relatesTo = null,
                transactionId = null,
                send = mock(),
            )
        )

        val cut = deviceVerificationViewModel()

        eventually(1.seconds) {
            cut.stack.value.active.configuration should beOfType<Config.SelectVerificationMethod>()
        }
    }

    @Test
    fun `redo self verification if the trust level of the device is not 'verified' after verification`() = runTest {
        every { onRedoSelfVerificationMock.invoke() } returns Unit
        every {
            keyServiceMock.getTrustLevel(ownUserId, ownDeviceId)
        } returns MutableStateFlow(DeviceTrustLevel.Valid(false))
        every { activeVerification.state } returns MutableStateFlow(
            ActiveVerificationState.Cancel(
                content = VerificationCancelEventContent(
                    VerificationCancelEventContent.Code.KeyMismatch, "", null, null
                ), isOurOwn = false
            )
        )

        val cut = deviceVerificationViewModel()
        settle()

        val deviceVerificationStepWrapper = cut.stack.value.active.instance
        deviceVerificationStepWrapper.shouldBeInstanceOf<VerificationViewModel.Wrapper.Cancelled>()
        deviceVerificationStepWrapper.viewModel.ok()
        settle()

        verify {
            onRedoSelfVerificationMock.invoke()
            onCloseDeviceVerificationMock.invoke()
        }
    }

    @Test
    fun `not redo self verification if the trust level of own device is 'verified' after verification`() = runTest {
        var onRedoWasCalled = false
        every { onRedoSelfVerificationMock.invoke() } calls {
            onRedoWasCalled = true
        }
        every {
            keyServiceMock.getTrustLevel(ownUserId, ownDeviceId)
        } returns MutableStateFlow(DeviceTrustLevel.Valid(false))
        every { activeVerification.state } returns MutableStateFlow(ActiveVerificationState.Done)

        val cut = deviceVerificationViewModel()

        eventually(3.seconds) {
            val deviceVerificationStepWrapper = cut.stack.value.active.instance
            deviceVerificationStepWrapper.shouldBeInstanceOf<VerificationViewModel.Wrapper.Success>()
            deviceVerificationStepWrapper.viewModel.ok()

            onRedoWasCalled shouldBe false
            verify {
                onCloseDeviceVerificationMock.invoke()
            }
        }
    }

    @Test
    fun `show request screen again when verification is re-initiated`() = runTest {
        every {
            keyServiceMock.getTrustLevel(ownUserId, ownDeviceId)
        } returns MutableStateFlow(DeviceTrustLevel.Valid(false))
        every { activeVerification.state } returns MutableStateFlow(ActiveVerificationState.Done)
        every { activeVerification2.state } returns MutableStateFlow(theirRequest())

        val cut = deviceVerificationViewModel()

        eventually(1.seconds) {
            cut.stack.value.active.configuration should beOfType<Config.Success>()
        }

        activeDeviceVerificationFlow.value = activeVerification2

        eventually(1.seconds) {
            cut.stack.value.active.configuration should beOfType<Config.Request>()
        }
    }

    @Test
    fun `cancel verification when no verification could be found`() = runTest {
        var cancelledVerification = false
        activeDeviceVerificationFlow.value = null
        every { onCloseDeviceVerificationMock.invoke() } calls {
            cancelledVerification = true
        }
        val cut = deviceVerificationViewModel()

        eventually(1.seconds) {
            cancelledVerification shouldBe true
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.deviceVerificationViewModel(): VerificationViewModel {
        Dispatchers.setMain(testDispatcher)
        return VerificationViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
            ),
            onCloseVerification = onCloseDeviceVerificationMock,
            onRedoSelfVerification = onRedoSelfVerificationMock,
            roomId = null,
            timelineEventId = null,
        )
    }

    private fun theirRequest() = ActiveVerificationState.TheirRequest(
        content = VerificationRequestToDeviceEventContent("", emptySet(), 0L, ""),
        ownDeviceId = ownDeviceId,
        supportedMethods = emptySet(),
        relatesTo = null,
        transactionId = null,
        send = mock(),
    )
}
