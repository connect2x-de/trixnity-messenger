package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.VerificationStepConfig
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.verification.ActiveVerification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.clientserverapi.client.DevicesApiClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.UsersApiClient
import net.folivo.trixnity.clientserverapi.model.devices.Device
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequestEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction1
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class VerificationViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    private val ownUserId = UserId("userId", "localhost")
    private val ownDeviceId = "deviceId"
    private val otherUserId = UserId("otherUserId")
    private val otherDeviceId = "otherDevice"

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var keyServiceMock: KeyService

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
    lateinit var devicesApiClientMock: DevicesApiClient

    @Mock
    lateinit var usersApiClientMock: UsersApiClient

    @Mock
    lateinit var getActiveVerification: GetActiveVerification

    @Mock
    lateinit var activeVerification: ActiveVerification

    @Mock
    lateinit var activeVerification2: ActiveVerification

//    private val activeDeviceVerificationMock = activeDeviceVerification(CoroutineScope())
//    {
//        every { state } returns activeVerificationState
//        every { theirUserId } returns UserId("otherUserId", "localhost")
//        every { theirDeviceId } returns "otherDeviceId"
//    }

//    private val activeDeviceVerificationStateFlowMock = MutableStateFlow(activeDeviceVerificationMock)

    private val onCloseDeviceVerificationMock = mockFunction0<Unit>(mocker)
    private val onRedoSelfVerificationMock = mockFunction0<Unit>(mocker)
    private lateinit var activeDeviceVerificationFlow: MutableStateFlow<ActiveVerification>

    init {
        Dispatchers.setMain(testMainDispatcher)
        coroutineTestScope = true
        isolationMode = IsolationMode.InstancePerTest

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            activeDeviceVerificationFlow = MutableStateFlow(activeVerification)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { keyServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.userId } returns ownUserId
                every { matrixClientMock.deviceId } returns ownDeviceId
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.json } returns Json
                every { matrixClientServerApiClientMock.devices } returns devicesApiClientMock
                every { matrixClientServerApiClientMock.users } returns usersApiClientMock

                everySuspending {
                    devicesApiClientMock.getDevice(isAny(), isNull())
                } returns Result.success(Device(ownDeviceId))
                everySuspending { usersApiClientMock.getDisplayName(isAny()) } returns Result.success("otherUser")

                every { getActiveVerification.activeDeviceVerification(isAny()) } returns activeDeviceVerificationFlow
                every { activeVerification.theirDeviceId } returns otherDeviceId
                every { activeVerification.theirUserId } returns otherUserId
                every { activeVerification2.theirDeviceId } returns otherDeviceId
                every { activeVerification2.theirUserId } returns otherUserId

                every { onCloseDeviceVerificationMock.invoke() } returns Unit
            }
        }

        should("show verification request when a verification is started by another client") {
            with(mocker) {
                every { activeVerification.state } returns MutableStateFlow(theirRequest())
            }

            val cut = deviceVerificationViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()

            eventually(1.seconds) {
                cut.stack.value.active.configuration should beOfType<VerificationStepConfig.Request>()
            }

            cancelNeverEndingCoroutines()
        }

        should("start verification if verification state is 'ready'") {
            with(mocker) {
                every { activeVerification.state } returns MutableStateFlow(
                    ActiveVerificationState.Ready(
                        ownDeviceId = ownDeviceId,
                        methods = emptySet(),
                        relatesTo = null,
                        transactionId = null,
                        send = mockFunction1(mocker),
                    )
                )
            }

            val cut = deviceVerificationViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()

            eventually(1.seconds) {
                cut.stack.value.active.configuration should beOfType<VerificationStepConfig.SelectVerificationMethod>()
            }

            cancelNeverEndingCoroutines()
        }

        should("redo self verification if the trust level of the device is not 'verified' after verification") {
            with(mocker) {
                every { onRedoSelfVerificationMock.invoke() } returns Unit
                every {
                    keyServiceMock.getTrustLevel(isEqual(ownUserId), isEqual(ownDeviceId))
                } returns MutableStateFlow(DeviceTrustLevel.Valid(false))
                every { activeVerification.state } returns MutableStateFlow(
                    ActiveVerificationState.Cancel(
                        content = VerificationCancelEventContent(
                            VerificationCancelEventContent.Code.KeyMismatch, "", null, null
                        ),
                        isOurOwn = false
                    )
                )
            }

            val cut = deviceVerificationViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()

            eventually(1.seconds) {
                val deviceVerificationStepWrapper = cut.stack.value.active.instance
                deviceVerificationStepWrapper.shouldBeInstanceOf<VerificationViewModel.VerificationStepWrapper.Cancelled>()
                deviceVerificationStepWrapper.verificationStepCancelledViewModel.ok()
            }
            testCoroutineScheduler.advanceUntilIdle()

            mocker.verify(exhaustive = false) {
                onRedoSelfVerificationMock.invoke()
                onCloseDeviceVerificationMock.invoke()
            }

            cancelNeverEndingCoroutines()
        }

        should("not redo self verification if the trust level of own device is 'verified' after verification") {
            var onRedoWasCalled = false
            with(mocker) {
                every { onRedoSelfVerificationMock.invoke() } runs {
                    onRedoWasCalled = true
                }
                every {
                    keyServiceMock.getTrustLevel(isEqual(ownUserId), isEqual(ownDeviceId))
                } returns MutableStateFlow(DeviceTrustLevel.Valid(false))
                every { activeVerification.state } returns MutableStateFlow(ActiveVerificationState.Done)
            }

            val cut = deviceVerificationViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()

            eventually(1.seconds) {
                val deviceVerificationStepWrapper = cut.stack.value.active.instance
                deviceVerificationStepWrapper.shouldBeInstanceOf<VerificationViewModel.VerificationStepWrapper.Success>()
                deviceVerificationStepWrapper.verificationStepSuccessViewModel.ok()
            }
            testCoroutineScheduler.advanceUntilIdle()

            onRedoWasCalled shouldBe false
            mocker.verify(exhaustive = false) {
                onCloseDeviceVerificationMock.invoke()
            }

            cancelNeverEndingCoroutines()
        }

        should("show request screen again, whe verification is re-initiated") {
            with(mocker) {
                every {
                    keyServiceMock.getTrustLevel(isEqual(ownUserId), isEqual(ownDeviceId))
                } returns MutableStateFlow(DeviceTrustLevel.Valid(false))
                every { activeVerification.state } returns MutableStateFlow(ActiveVerificationState.Done)
                every { activeVerification2.state } returns MutableStateFlow(theirRequest())
            }

            val cut = deviceVerificationViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()

            eventually(1.seconds) {
                cut.stack.value.active.configuration should beOfType<VerificationStepConfig.Success>()
            }

            activeDeviceVerificationFlow.value = activeVerification2
            testCoroutineScheduler.advanceUntilIdle()

            eventually(1.seconds) {
                cut.stack.value.active.configuration should beOfType<VerificationStepConfig.Request>()
            }

            cancelNeverEndingCoroutines()
        }

    }

    private fun deviceVerificationViewModel(coroutineContext: CoroutineContext): VerificationViewModel {
        return VerificationViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(trixnityMessengerModule(), testMatrixClientModule(matrixClientMock), module {
                        single { getActiveVerification }
                    })
                }.koin,
                accountName = "test",
                coroutineContext = coroutineContext
            ),
            onCloseVerification = onCloseDeviceVerificationMock,
            onRedoSelfVerification = onRedoSelfVerificationMock,
            roomId = null,
            timelineEventId = null,
        )
    }

    private fun theirRequest() = ActiveVerificationState.TheirRequest(
        content = VerificationRequestEventContent("", emptySet(), 0L, ""),
        ownDeviceId = ownDeviceId,
        supportedMethods = emptySet(),
        relatesTo = null,
        transactionId = null,
        send = mockFunction1(mocker),
    )
}
