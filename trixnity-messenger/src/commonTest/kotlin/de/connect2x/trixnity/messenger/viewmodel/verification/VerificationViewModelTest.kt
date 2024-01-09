package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.VerificationStepConfig
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.DeviceTrustLevel
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
    override fun timeout(): Long = 4_000

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
    lateinit var devicesApiClientMock: DeviceApiClient

    @Mock
    lateinit var usersApiClientMock: UserApiClient

    @Mock
    lateinit var verificationService: VerificationService

    @Mock
    lateinit var activeVerification: ActiveDeviceVerification

    @Mock
    lateinit var activeVerification2: ActiveDeviceVerification

    private val onCloseDeviceVerificationMock = mockFunction0<Unit>(mocker)
    private val onRedoSelfVerificationMock = mockFunction0<Unit>(mocker)
    private lateinit var activeDeviceVerificationFlow: MutableStateFlow<ActiveDeviceVerification>

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
                            single { verificationService }
                        }
                    )
                }.koin
                every { matrixClientMock.userId } returns ownUserId
                every { matrixClientMock.deviceId } returns ownDeviceId
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.json } returns Json
                every { matrixClientServerApiClientMock.device } returns devicesApiClientMock
                every { matrixClientServerApiClientMock.user } returns usersApiClientMock

                everySuspending {
                    devicesApiClientMock.getDevice(isAny(), isNull())
                } returns Result.success(Device(ownDeviceId))
                everySuspending { usersApiClientMock.getDisplayName(isAny()) } returns Result.success("otherUser")

                every { verificationService.activeDeviceVerification } returns activeDeviceVerificationFlow
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

            val coroutineScope = CoroutineScope(Dispatchers.Default)
            coroutineScope.launch {
                eventually(1.seconds) {
                    cut.stack.value.active.configuration should beOfType<VerificationStepConfig.Request>()
                }
            }.join()

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

            val coroutineScope = CoroutineScope(Dispatchers.Default)
            coroutineScope.launch {
                eventually(1.seconds) {
                    cut.stack.value.active.configuration should beOfType<VerificationStepConfig.SelectVerificationMethod>()
                }
            }.join()

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

            val coroutineScope = CoroutineScope(Dispatchers.Default)
            coroutineScope.launch {
                eventually(1.seconds) {
                    val deviceVerificationStepWrapper = cut.stack.value.active.instance
                    deviceVerificationStepWrapper.shouldBeInstanceOf<VerificationViewModel.VerificationStepWrapper.Cancelled>()
                    deviceVerificationStepWrapper.verificationStepCancelledViewModel.ok()
                }
            }.join()
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

            val coroutineScope = CoroutineScope(Dispatchers.Default)
            coroutineScope.launch {
                eventually(1.seconds) {
                    val deviceVerificationStepWrapper = cut.stack.value.active.instance
                    deviceVerificationStepWrapper.shouldBeInstanceOf<VerificationViewModel.VerificationStepWrapper.Success>()
                    deviceVerificationStepWrapper.verificationStepSuccessViewModel.ok()
                }
            }.join()
            testCoroutineScheduler.advanceUntilIdle()

            onRedoWasCalled shouldBe false
            mocker.verify(exhaustive = false) {
                onCloseDeviceVerificationMock.invoke()
            }

            cancelNeverEndingCoroutines()
        }

        should("show request screen again, when verification is re-initiated") {
            with(mocker) {
                every {
                    keyServiceMock.getTrustLevel(isEqual(ownUserId), isEqual(ownDeviceId))
                } returns MutableStateFlow(DeviceTrustLevel.Valid(false))
                every { activeVerification.state } returns MutableStateFlow(ActiveVerificationState.Done)
                every { activeVerification2.state } returns MutableStateFlow(theirRequest())
            }

            println(0)
            val cut = deviceVerificationViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()

            println(1)
            eventually(1.seconds) {
                cut.stack.value.active.configuration should beOfType<VerificationStepConfig.Success>()
            }

            println(2)
            activeDeviceVerificationFlow.value = activeVerification2
            testCoroutineScheduler.advanceUntilIdle()

            println(3)
            eventually(1.seconds) {
                cut.stack.value.active.configuration should beOfType<VerificationStepConfig.Request>()
            }

            println(4)
            cancelNeverEndingCoroutines()
        }

    }

    private fun deviceVerificationViewModel(coroutineContext: CoroutineContext): VerificationViewModel {
        return VerificationViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
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
        send = mockFunction1(mocker),
    )
}
