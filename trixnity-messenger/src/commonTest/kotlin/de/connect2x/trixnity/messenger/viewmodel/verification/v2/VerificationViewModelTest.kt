package de.connect2x.trixnity.messenger.viewmodel.verification.v2

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.key.KeyService
import de.connect2x.trixnity.client.verification.ActiveDeviceVerification
import de.connect2x.trixnity.client.verification.ActiveSasVerificationState
import de.connect2x.trixnity.client.verification.ActiveVerificationState
import de.connect2x.trixnity.client.verification.VerificationService
import de.connect2x.trixnity.clientserverapi.client.DeviceApiClient
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.UserApiClient
import de.connect2x.trixnity.clientserverapi.model.device.Device
import de.connect2x.trixnity.core.ErrorResponse
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.Profile
import de.connect2x.trixnity.core.model.ProfileField
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationMethod
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationRequestToDeviceEventContent
import de.connect2x.trixnity.crypto.key.DeviceTrustLevel
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.settle
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.GetAccountProfileDisplayName
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class VerificationViewModelTest {

    private val ownUserId = UserId("userId", "localhost")
    private val ownDeviceId = "deviceId"
    private val ownDisplayName = "myDisplayName"
    private val ownDeviceDisplayName = "myDevice"
    private val otherUserId = UserId("otherUserId")
    private val otherDeviceId = "otherDevice"
    private val otherDisplayName = "otherDisplayName"
    private val otherDeviceDisplayName = "otherDeviceDisplayName"

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
    private val onStepChangeMock = mock<Function1<ActiveVerificationState, Unit>>()
    private val onSasStepChangeMock = mock<Function1<ActiveSasVerificationState, Unit>>()
    private val activeDeviceVerificationFlow: MutableStateFlow<ActiveDeviceVerification?> =
        MutableStateFlow(activeVerification)
    private val getAccountProfileDisplayNameMock = mock<GetAccountProfileDisplayName>()

    init {
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { keyServiceMock }
                            single { verificationService }
                        }
                    )
                }
                .koin
        every { matrixClientMock.userId } returns ownUserId
        every { matrixClientMock.deviceId } returns ownDeviceId
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        val profile = Profile(ProfileField.DisplayName(otherDisplayName))
        every { matrixClientMock.profile } returns MutableStateFlow(profile)
        every { matrixClientServerApiClientMock.json } returns Json
        every { matrixClientServerApiClientMock.device } returns devicesApiClientMock
        every { matrixClientServerApiClientMock.user } returns usersApiClientMock

        everySuspend { devicesApiClientMock.getDevice(ownDeviceId) } returns
            Result.success(Device(ownDeviceId, displayName = ownDeviceDisplayName))
        everySuspend { devicesApiClientMock.getDevice(otherDeviceId) } returns
            Result.success(Device(otherDeviceId, displayName = otherDeviceDisplayName))
        everySuspend { usersApiClientMock.getProfileField(otherUserId, ProfileField.DisplayName) } returns
            Result.success(ProfileField.DisplayName(otherDisplayName))

        every { verificationService.activeDeviceVerification } returns activeDeviceVerificationFlow
        every { activeVerification.theirDeviceId } returns otherDeviceId
        every { activeVerification.theirUserId } returns otherUserId
        every { activeVerification2.theirDeviceId } returns otherDeviceId
        every { activeVerification2.theirUserId } returns otherUserId
        every { getAccountProfileDisplayNameMock.fromMatrixClient(any()) } returns flowOf(ownDisplayName)
        every { onCloseDeviceVerificationMock.invoke() } returns Unit
        every { onStepChangeMock.invoke(any()) } returns Unit
        every { onSasStepChangeMock.invoke(any()) } returns Unit
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `show verification request when a verification is started by another client`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(theirRequest())

        val cut = deviceVerificationViewModel()

        eventually(1.seconds) {
            cut.activeVerificationState.value.shouldBeInstanceOf<ActiveVerificationState.TheirRequest>()
        }
    }

    @Test
    fun `start verification if verification state is 'ready'`() = runTest {
        every { activeVerification.state } returns
            MutableStateFlow(
                ActiveVerificationState.Ready(
                    ownDeviceId = ownDeviceId,
                    methods = emptySet(),
                    relatesTo = null,
                    transactionId = null,
                    send = mock(),
                )
            )

        val cut = deviceVerificationViewModel()

        eventually(1.seconds) { cut.activeVerificationState.value.shouldBeInstanceOf<ActiveVerificationState.Ready>() }
    }

    @Test
    fun `gather verification methods if verification state is 'ready'`() = runTest {
        val activeVerificationState =
            MutableStateFlow<ActiveVerificationState>(
                ActiveVerificationState.Ready(
                    ownDeviceId = ownDeviceId,
                    methods = setOf(VerificationMethod.Sas),
                    relatesTo = null,
                    transactionId = null,
                    send = mock(),
                )
            )
        every { activeVerification.state } returns activeVerificationState

        val cut = deviceVerificationViewModel()

        cut.verificationMethods.launchIn(backgroundScope)
        eventually(1.seconds) {
            cut.activeVerificationState.value.shouldBeInstanceOf<ActiveVerificationState.Ready>()
            cut.verificationMethods.value?.map { it.first } shouldBe listOf(VerificationMethod.Sas)
        }
        activeVerificationState.value = ActiveVerificationState.Undefined
        eventually(1.seconds) { cut.verificationMethods.value.shouldBeNull() }
    }

    @Test
    fun `start verification on accepting verification method`() = runTest {
        every { activeVerification.state } returns
            MutableStateFlow(
                ActiveVerificationState.Ready(
                    ownDeviceId = ownDeviceId,
                    methods = setOf(VerificationMethod.Sas),
                    relatesTo = null,
                    transactionId = null,
                    send = mock(mode = MockMode.autofill),
                )
            )

        val cut = deviceVerificationViewModel()

        val verificationState =
            eventually(1.seconds) {
                cut.activeVerificationState.value.shouldBeInstanceOf<ActiveVerificationState.Ready>()
            }

        cut.acceptVerificationMethod(VerificationMethod.Sas)
        delay(500.milliseconds)
        verifySuspend(VerifyMode.exactly(1)) { verificationState.start(VerificationMethod.Sas) }
    }

    @Test
    fun `redo self verification if the trust level of the device is not 'verified' after verification`() = runTest {
        every { onRedoSelfVerificationMock.invoke() } returns Unit
        every { keyServiceMock.getTrustLevel(ownUserId, ownDeviceId) } returns
            MutableStateFlow(DeviceTrustLevel.Valid(false))
        every { activeVerification.state } returns
            MutableStateFlow(
                ActiveVerificationState.Cancel(
                    content =
                        VerificationCancelEventContent(VerificationCancelEventContent.Code.KeyMismatch, "", null, null),
                    isOurOwn = false,
                )
            )

        val cut = deviceVerificationViewModel()
        settle()

        val deviceVerificationStepWrapper = cut.activeVerificationState.value
        deviceVerificationStepWrapper.shouldBeInstanceOf<ActiveVerificationState.Cancel>()
        cut.ok()
        settle()

        verify {
            onRedoSelfVerificationMock.invoke()
            onCloseDeviceVerificationMock.invoke()
        }
    }

    @Test
    fun `not redo self verification if the trust level of own device is 'verified' after verification`() = runTest {
        var onRedoWasCalled = false
        every { onRedoSelfVerificationMock.invoke() } calls { onRedoWasCalled = true }
        every { keyServiceMock.getTrustLevel(ownUserId, ownDeviceId) } returns
            MutableStateFlow(DeviceTrustLevel.Valid(false))
        every { activeVerification.state } returns MutableStateFlow(ActiveVerificationState.Done)

        val cut = deviceVerificationViewModel()

        eventually(3.seconds) {
            val deviceVerificationStepWrapper = cut.activeVerificationState.value
            deviceVerificationStepWrapper.shouldBeInstanceOf<ActiveVerificationState.Done>()
            cut.ok()

            onRedoWasCalled shouldBe false
            verify { onCloseDeviceVerificationMock.invoke() }
        }
    }

    @Test
    fun `show request screen again when verification is re-initiated`() = runTest {
        every { keyServiceMock.getTrustLevel(ownUserId, ownDeviceId) } returns
            MutableStateFlow(DeviceTrustLevel.Valid(false))
        every { activeVerification.state } returns MutableStateFlow(ActiveVerificationState.Done)
        every { activeVerification2.state } returns MutableStateFlow(theirRequest())

        val cut = deviceVerificationViewModel()

        eventually(1.seconds) { cut.activeVerificationState.value.shouldBeInstanceOf<ActiveVerificationState.Done>() }

        activeDeviceVerificationFlow.value = activeVerification2

        eventually(1.seconds) {
            cut.activeVerificationState.value.shouldBeInstanceOf<ActiveVerificationState.TheirRequest>()
        }
    }

    @Test
    fun `cancel verification when no verification could be found`() = runTest {
        var cancelledVerification = false
        activeDeviceVerificationFlow.value = null
        every { onCloseDeviceVerificationMock.invoke() } calls { cancelledVerification = true }
        val cut = deviceVerificationViewModel()

        eventually(1.seconds) { cancelledVerification shouldBe true }
    }

    @Test
    fun `get own user data`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(theirRequest())
        val cut = deviceVerificationViewModel()
        backgroundScope.launch { cut.ourDisplayName.collect() }
        backgroundScope.launch { cut.ourDeviceDisplayName.collect() }
        eventually(1.seconds) {
            cut.ourUserId shouldBe ownUserId
            cut.ourDisplayName.value shouldBe ownDisplayName
            cut.ourDeviceDisplayName.value shouldBe ownDeviceDisplayName
        }
    }

    @Test
    fun `get external user data`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(theirRequest())
        val cut = deviceVerificationViewModel()
        backgroundScope.launch { cut.theirDisplayName.collect() }
        backgroundScope.launch { cut.theirDeviceDisplayName.collect() }
        eventually(1.seconds) {
            cut.theirUserId.value shouldBe otherUserId
            cut.theirDisplayName.value shouldBe otherDisplayName
            cut.theirDeviceDisplayName.value shouldBe otherDeviceDisplayName
        }
    }

    @Test
    fun `return default when fetching external user display name on denied access`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(theirRequest())
        val cut = deviceVerificationViewModel()
        everySuspend { usersApiClientMock.getProfileField(otherUserId, ProfileField.DisplayName) } returns
            responseForbidden()
        backgroundScope.launch { cut.theirDisplayName.collect() }
        eventually(1.seconds) { cut.theirDisplayName.value shouldBe otherUserId.full }
    }

    @Test
    fun `return default when fetching own device display name on denied access`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(theirRequest())
        val cut = deviceVerificationViewModel()
        everySuspend { devicesApiClientMock.getDevice(ownDeviceId) } returns responseForbidden()
        backgroundScope.launch { cut.ourDeviceDisplayName.collect() }
        eventually(1.seconds) { cut.ourDeviceDisplayName.value shouldBe ownDeviceId }
    }

    @Test
    fun `return default when fetching external device display name on denied access`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(theirRequest())
        val cut = deviceVerificationViewModel()
        everySuspend { devicesApiClientMock.getDevice(otherDeviceId) } returns responseForbidden()
        backgroundScope.launch { cut.theirDeviceDisplayName.collect() }
        eventually(1.seconds) { cut.theirDeviceDisplayName.value shouldBe otherDeviceId }
    }

    @Test
    fun `recognize if request is from the same account`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(theirRequest())
        every { activeVerification.theirUserId } returns ownUserId
        every { activeVerification.theirDeviceId } returns ownDeviceId
        val cut = deviceVerificationViewModel()
        cut.isFromOwnAccount.launchIn(backgroundScope)
        eventually(1.seconds) { cut.isFromOwnAccount.value shouldBe true }
    }

    @Test
    fun `recognize if request is from a different account`() = runTest {
        every { activeVerification.state } returns MutableStateFlow(theirRequest())
        val cut = deviceVerificationViewModel()
        cut.isFromOwnAccount.launchIn(backgroundScope)
        eventually(1.seconds) { cut.isFromOwnAccount.value shouldBe false }
    }

    private fun <T> responseForbidden(): Result<T> =
        Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("403")))

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.deviceVerificationViewModel(): VerificationViewModel {
        Dispatchers.setMain(testDispatcher)
        return VerificationViewModelImpl(
            viewModelContext =
                testMatrixClientViewModelContext(
                    di =
                        koinApplication {
                                modules(createTestDefaultTrixnityMessengerModules(mapOf(ownUserId to matrixClientMock)))
                            }
                            .koin,
                    userId = ownUserId,
                ),
            onCloseVerification = onCloseDeviceVerificationMock,
            onRedoSelfVerification = onRedoSelfVerificationMock,
            onStateChange = onStepChangeMock,
            onSasStateChange = onSasStepChangeMock,
            roomId = null,
            timelineEventId = null,
            getAccountProfileDisplayName = getAccountProfileDisplayNameMock,
        )
    }

    private fun theirRequest() =
        ActiveVerificationState.TheirRequest(
            content = VerificationRequestToDeviceEventContent("", emptySet(), 0L, ""),
            ownDeviceId = ownDeviceId,
            supportedMethods = emptySet(),
            relatesTo = null,
            transactionId = null,
            send = mock(),
        )
}
