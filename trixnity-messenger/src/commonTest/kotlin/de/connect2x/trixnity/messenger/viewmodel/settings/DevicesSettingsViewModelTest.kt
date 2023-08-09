package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.util.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.KeyTrustService
import net.folivo.trixnity.client.store.KeyStore
import net.folivo.trixnity.client.store.repository.*
import net.folivo.trixnity.client.verification.ActiveDeviceVerification
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.clientserverapi.client.DevicesApiClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.devices.Device
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import net.folivo.trixnity.clientserverapi.model.uia.UIAState
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequestEventContent
import net.folivo.trixnity.core.model.keys.DeviceKeys
import net.folivo.trixnity.core.model.keys.Keys
import net.folivo.trixnity.crypto.olm.OlmDecrypter
import net.folivo.trixnity.crypto.olm.OlmEncryptionService
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction1
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class DevicesSettingsViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    private val ourDeviceId = "deviceId1"
    private val ourDeviceId2 = "deviceId21"
    private val ourUserId = UserId("userId1", "localhost")
    private val ourUserId2 = UserId("userId21", "localhost")

    private val device1 = Device(
        ourDeviceId,
        "device1",
        "1.2.3.4",
        LocalDateTime.parse("2021-12-10T09:50:00.00").toInstant(TimeZone.UTC).toEpochMilliseconds()
    )

    private val device2 = Device(
        "deviceId2",
        "device2",
        "4.3.2.1",
        LocalDateTime.parse("2021-12-10T07:50:00.00").toInstant(TimeZone.UTC).toEpochMilliseconds()
    )

    private val device21 = Device(
        ourDeviceId2,
        "device22",
        "0.9.8.7",
        LocalDateTime.parse("2021-12-10T09:50:00.00").toInstant(TimeZone.UTC).toEpochMilliseconds(),
    )

    private val device22 = Device(
        "deviceId22",
        "device22",
        "7.8.9.0",
        LocalDateTime.parse("2021-12-10T07:50:00.00").toInstant(TimeZone.UTC).toEpochMilliseconds()
    )

    private val deviceKeys = flowOf(
        listOf(DeviceKeys(ourUserId, "deviceId", setOf(), Keys(setOf())))
    )

    private val deviceKeys2 = flowOf(
        listOf(DeviceKeys(ourUserId2, "deviceId", setOf(), Keys(setOf())))
    )

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var matrixClientMock2: MatrixClient

    @Mock
    lateinit var keyServiceMock: KeyService

    @Mock
    lateinit var keyServiceMock2: KeyService

    @Mock
    lateinit var verificationServiceMock: VerificationService

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
    lateinit var matrixClientServerApiClientMock2: MatrixClientServerApiClient

    @Mock
    lateinit var devicesApiClientMock: DevicesApiClient

    @Mock
    lateinit var devicesApiClientMock2: DevicesApiClient

    @Mock
    lateinit var olmDecrypterMock: OlmDecrypter

    @Mock
    lateinit var olmEncryptionServiceMock: OlmEncryptionService

    @Mock
    lateinit var keyTrustServiceMock: KeyTrustService

    private lateinit var updateDeviceMocker: Mocker.EverySuspend<Result<Unit>>
    private lateinit var deviceKeysMocker: Mocker.Every<Flow<List<DeviceKeys>?>>

    init {
        Dispatchers.setMain(testMainDispatcher)
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { keyServiceMock }
                            single { verificationServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.deviceId } returns ourDeviceId
                every { matrixClientMock.userId } returns ourUserId
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.devices } returns devicesApiClientMock
                every { matrixClientServerApiClientMock.json } returns Json

                every { matrixClientMock2.di } returns koinApplication {
                    modules(
                        module {
                            single { keyServiceMock2 }
                            single { verificationServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock2.deviceId } returns ourDeviceId2
                every { matrixClientMock2.userId } returns ourUserId2
                every { matrixClientMock2.api } returns matrixClientServerApiClientMock2
                every { matrixClientServerApiClientMock2.devices } returns devicesApiClientMock2
                every { matrixClientServerApiClientMock2.json } returns Json

                updateDeviceMocker = everySuspending {
                    devicesApiClientMock.updateDevice(isAny(), isAny(), isNull())
                }
                updateDeviceMocker returns Result.success(Unit)

                deviceKeysMocker = every { keyServiceMock.getDeviceKeys(isEqual(ourUserId)) }
                deviceKeysMocker returns deviceKeys
                every { keyServiceMock2.getDeviceKeys(isEqual(ourUserId2)) } returns deviceKeys2
                // standard values for mock2, we do not need any config here
                every {
                    keyServiceMock2.getTrustLevel(isAny<UserId>(), isAny())
                } returns flowOf(DeviceTrustLevel.Valid(true))
                everySuspending { devicesApiClientMock2.getDevices() } returns Result.success(
                    listOf(device21, device22)
                )
            }
        }

        should("load devices initially") {
            with(mocker) {
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual(ourDeviceId))
                } returns flowOf(DeviceTrustLevel.Valid(true))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual("deviceId2"))
                } returns flowOf(DeviceTrustLevel.NotCrossSigned)
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            testCoroutineScheduler.advanceUntilIdle()
            accountsWithDevices.first { it.size == 2 }

            assertSoftly(accountsWithDevices.value) {
                get(0).accountName shouldBe "test"
                get(1).accountName shouldBe "test2"

                get(0).isLoading.first { it.not() }
                get(0).devicesInAccount.first { it.thisDevice.deviceId == ourDeviceId }
                assertSoftly(get(0).devicesInAccount.value.thisDevice) {
                    deviceId shouldBe ourDeviceId
                    displayName.value shouldBe "device1"
                    lastSeenAt shouldBe "last seen: 12/10/2021"
                    isVerified.value shouldBe true
                }
                get(0).devicesInAccount.value.otherDevices shouldHaveSize 1
                assertSoftly(get(0).devicesInAccount.value.otherDevices[0]) {
                    deviceId shouldBe "deviceId2"
                    displayName.value shouldBe "device2"
                    lastSeenAt shouldBe "last seen: 12/10/2021"
                    isVerified.value shouldBe false
                }

                get(1).devicesInAccount.first { it.thisDevice.deviceId == ourDeviceId2 }
                get(1).devicesInAccount.value.thisDevice.deviceId shouldBe ourDeviceId2
                get(1).devicesInAccount.value.otherDevices shouldHaveSize 1
            }
            cancelNeverEndingCoroutines()
        }

        should("react to changes in the trust level of devices") {
            val trustLevel1 = MutableStateFlow<DeviceTrustLevel>(DeviceTrustLevel.CrossSigned(true))
            val trustLevel2 = MutableStateFlow<DeviceTrustLevel>(DeviceTrustLevel.NotCrossSigned)
            with(mocker) {
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual(ourDeviceId))
                } returns trustLevel1
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual("deviceId2"))
                } returns trustLevel2
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices

            trustLevel1.value = DeviceTrustLevel.CrossSigned(false)
            trustLevel2.value = DeviceTrustLevel.CrossSigned(true)
            testCoroutineScheduler.advanceUntilIdle()

            accountsWithDevices.first { it.size == 2 }

            assertSoftly(accountsWithDevices.value) {
                get(0).devicesInAccount.first { it.thisDevice.deviceId == ourDeviceId }
                assertSoftly(get(0).devicesInAccount.value) {
                    thisDevice.isVerified.value shouldBe false
                    otherDevices[0].isVerified.value shouldBe true
                }
            }

            cancelNeverEndingCoroutines()
        }

        should("react to changes in the devices list") {
            val deviceKeysList = MutableStateFlow(
                listOf(
                    DeviceKeys(
                        userId = ourUserId,
                        deviceId = "deviceId",
                        algorithms = setOf(),
                        keys = Keys(setOf()),
                    )
                )
            )
            deviceKeysMocker returns deviceKeysList

            val getDevicesMocker = mocker.everySuspending { devicesApiClientMock.getDevices() }
            getDevicesMocker returns Result.success(listOf(device1, device2))
            with(mocker) {
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual(ourDeviceId))
                } returns flowOf(DeviceTrustLevel.Valid(true))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual("deviceId2"))
                } returns flowOf(DeviceTrustLevel.NotCrossSigned)
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            testCoroutineScheduler.advanceUntilIdle()

            accountsWithDevices.first {
                it.isNotEmpty() &&
                        it[0].devicesInAccount.value.otherDevices.isNotEmpty() &&
                        it[0].devicesInAccount.value.otherDevices[0].displayName.value == "device2"
            }

            getDevicesMocker returns Result.success(
                listOf(
                    device1,
                    device2.copy(displayName = "device2___new")
                )
            )

            deviceKeysList.value = deviceKeysList.value + DeviceKeys(ourUserId, ourDeviceId, setOf(), Keys(setOf()))
            testCoroutineScheduler.advanceUntilIdle()
            accountsWithDevices.first {
                it.isNotEmpty() &&
                        it[0].devicesInAccount.value.otherDevices.isNotEmpty() &&
                        it[0].devicesInAccount.value.otherDevices[0].displayName.value == "device2___new"
            }

            cancelNeverEndingCoroutines()
        }

        should("show an error message if loading devices cannot be performed") {
            mocker.everySuspending { devicesApiClientMock.getDevices() } returns Result.failure(RuntimeException("Oh no!"))

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            testCoroutineScheduler.advanceUntilIdle()

            accountsWithDevices.first {
                it.isNotEmpty()
                        && it[0].loadingError.value != null
            }

            cancelNeverEndingCoroutines()
        }

        should("set the display name for this device and update the device") {
            with(mocker) {
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isAny<UserId>(), isAny())
                } returns flowOf(DeviceTrustLevel.Valid(true))
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            // wait until initial computation is done
            val accountsWithDevices = cut.accountsWithDevices
            testCoroutineScheduler.advanceUntilIdle()
            accountsWithDevices.first { it.isNotEmpty() && it[0].devicesInAccount.value.thisDevice.deviceId == ourDeviceId }

            cut.setDisplayName("test", ourDeviceId, "device1", "device1 updated")
            testCoroutineScheduler.advanceUntilIdle()

            accountsWithDevices.first {
                it.isNotEmpty() &&
                        it[0].devicesInAccount.value.thisDevice.displayName.value == "device1 updated"
            }

            cancelNeverEndingCoroutines()
        }

        should("display error message when device cannot be renamed") {
            with(mocker) {
                updateDeviceMocker returns Result.failure(RuntimeException("Oh no!"))
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isAny<UserId>(), isAny())
                } returns flowOf(DeviceTrustLevel.Valid(true))
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            testCoroutineScheduler.advanceUntilIdle()
            accountsWithDevices.first { it.isNotEmpty() && it[0].devicesInAccount.value.thisDevice.deviceId == ourDeviceId }

            cut.error.value shouldBe null
            cut.setDisplayName("test", ourDeviceId, "device1", "device1 updated")
            testCoroutineScheduler.advanceUntilIdle()

            cut.error.value shouldNotBe null

            cancelNeverEndingCoroutines()
        }

        should("initiate a verification request") {
            with(mocker) {
                everySuspending {
                    verificationServiceMock.createDeviceVerificationRequest(isEqual(ourUserId), isAny())
                } returns Result.success(activeDeviceVerification(CoroutineScope(testCoroutineScheduler)))
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual(ourDeviceId))
                } returns flowOf(DeviceTrustLevel.Valid(true))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual("deviceId2"))
                } returns flowOf(DeviceTrustLevel.NotCrossSigned)
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            accountsWithDevices.first { it.isNotEmpty() && it[0].devicesInAccount.value.thisDevice.deviceId == ourDeviceId }

            cut.verify("test", "deviceId2")
            testCoroutineScheduler.advanceUntilIdle()

            mocker.verifyWithSuspend(exhaustive = false) {
                verificationServiceMock.createDeviceVerificationRequest(
                    isEqual(ourUserId),
                    isEqual(setOf("deviceId2")),
                )
            }

            cancelNeverEndingCoroutines()
        }

        should("show an error message when verification cannot be performed") {
            with(mocker) {
                everySuspending {
                    verificationServiceMock.createDeviceVerificationRequest(isEqual(ourUserId), isAny())
                } runs { throw RuntimeException("Oh no!") }
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual(ourDeviceId))
                } returns flowOf(DeviceTrustLevel.Valid(true))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual("deviceId2"))
                } returns flowOf(DeviceTrustLevel.NotCrossSigned)
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            accountsWithDevices.first { it.isNotEmpty() && it[0].devicesInAccount.value.thisDevice.deviceId == ourDeviceId }
            cut.error.value shouldBe null
            cut.verify("test", "deviceId2")
            testCoroutineScheduler.advanceUntilIdle()

            cut.error.value shouldNotBe null

            cancelNeverEndingCoroutines()
        }

        should("show password prompt when trying to remove a device") {
            with(mocker) {
                everySuspending { devicesApiClientMock.deleteDevice("deviceId2") } returns Result.success(
                    UIA.Step(
                        state = UIAState(
                            flows = setOf(
                                UIAState.FlowInformation(
                                    stages = listOf(AuthenticationType.Password)
                                )
                            )
                        ),
                        getFallbackUrlCallback = mockFunction1(mocker),
                        authenticateCallback = mockFunction1(mocker),
                        onSuccessCallback = mockFunction0(mocker) {},
                    )
                )
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual(ourDeviceId))
                } returns flowOf(DeviceTrustLevel.Valid(true))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual("deviceId2"))
                } returns flowOf(DeviceTrustLevel.NotCrossSigned)
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            accountsWithDevices.first { it.isNotEmpty() && it[0].devicesInAccount.value.thisDevice.deviceId == ourDeviceId }

            cut.showLogin.value shouldBe null
            cut.remove("test", "deviceId2")
            testCoroutineScheduler.advanceUntilIdle()

            cut.showLogin.value shouldNotBe null

            cancelNeverEndingCoroutines()
        }

        should("show an error message when the device could not be removed") {
            with(mocker) {
                everySuspending { devicesApiClientMock.deleteDevice("deviceId2") } returns Result.success(
                    UIA.Error(
                        state = UIAState(),
                        getFallbackUrlCallback = mockFunction1(mocker),
                        errorResponse = ErrorResponse.CustomErrorResponse(""),
                        authenticateCallback = mockFunction1(mocker),
                        onSuccessCallback = mockFunction0(mocker) { },
                    )
                )
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual(ourDeviceId))
                } returns flowOf(DeviceTrustLevel.Valid(true))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual("deviceId2"))
                } returns flowOf(DeviceTrustLevel.NotCrossSigned)
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            accountsWithDevices.first { it.isNotEmpty() && it[0].devicesInAccount.value.thisDevice.deviceId == ourDeviceId }

            cut.removeError.value shouldBe null
            cut.remove("test", "deviceId2")
            testCoroutineScheduler.advanceUntilIdle()

            cut.removeError.value shouldNotBe null

            cancelNeverEndingCoroutines()
        }

        should("authenticate with password when trying to remove a device") {
            var authenticateWasCalled = false
            with(mocker) {
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1))
                everySuspending { devicesApiClientMock.deleteDevice("deviceId2") } returns Result.success(
                    UIA.Step(
                        state = UIAState(
                            flows = setOf(
                                UIAState.FlowInformation(
                                    stages = listOf(AuthenticationType.Password)
                                )
                            )
                        ),
                        getFallbackUrlCallback = mockFunction1(mocker),
                        authenticateCallback = mockFunction1(mocker) {
                            authenticateWasCalled = true
                            Result.success(UIA.Success(Unit))
                        },
                        onSuccessCallback = mockFunction0(mocker) {},
                    )
                )
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual(ourDeviceId))
                } returns flowOf(DeviceTrustLevel.Valid(true))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual("deviceId2"))
                } returns flowOf(DeviceTrustLevel.NotCrossSigned)
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            accountsWithDevices.first { it.isNotEmpty() && it[0].devicesInAccount.value.thisDevice.deviceId == ourDeviceId }

            cut.showRemoveDevice.value = "deviceId2" // triggered by UI
            cut.remove("test", "deviceId2")
            testCoroutineScheduler.advanceUntilIdle()
            cut.authenticate("test", "p4ssw0rd!", "deviceId2")
            testCoroutineScheduler.advanceUntilIdle()

            authenticateWasCalled shouldBe true
            cut.showLogin.value shouldBe null
            cut.showRemoveDevice.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("show 'password wrong' when login attempt with password fails and reset when dialog closes") {
            val authenticateCallback: suspend (p: AuthenticationRequest) -> Result<UIA<Unit>> = {
                Result.success(
                    UIA.Error(
                        state = UIAState(),
                        getFallbackUrlCallback = mockFunction1(mocker),
                        errorResponse = ErrorResponse.Forbidden(),
                        authenticateCallback = mockFunction1(mocker),
                        onSuccessCallback = mockFunction0(mocker),
                    )
                )
            }
            with(mocker) {
                everySuspending { devicesApiClientMock.deleteDevice("deviceId2") } returns Result.success(
                    UIA.Step(
                        state = UIAState(
                            flows = setOf(
                                UIAState.FlowInformation(
                                    stages = listOf(AuthenticationType.Password)
                                )
                            )
                        ),
                        getFallbackUrlCallback = mockFunction1(mocker),
                        authenticateCallback = authenticateCallback,
                        onSuccessCallback = mockFunction0(mocker),
                    )
                )
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual(ourDeviceId))
                } returns flowOf(DeviceTrustLevel.Valid(true))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual("deviceId2"))
                } returns flowOf(DeviceTrustLevel.NotCrossSigned)
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            accountsWithDevices.first { it.isNotEmpty() && it[0].devicesInAccount.value.thisDevice.deviceId == ourDeviceId }

            cut.passwordWrong.value shouldBe false
            cut.showRemoveDevice.value = "deviceId2" // triggered by UI
            cut.remove("test", "deviceId2")
            testCoroutineScheduler.advanceUntilIdle()
            cut.authenticate("test", "p4ssw0rd!", "deviceId2")
            testCoroutineScheduler.advanceUntilIdle()

            cut.passwordWrong.value shouldBe true

            cut.closeRemoveDialog()
            cut.passwordWrong.value shouldBe false

            cancelNeverEndingCoroutines()
        }

        should("show an error message when the authentication with password cannot be performed") {
            with(mocker) {
                everySuspending { devicesApiClientMock.deleteDevice("deviceId2") } returns Result.success(
                    UIA.Step(
                        state = UIAState(
                            flows = setOf(
                                UIAState.FlowInformation(
                                    stages = listOf(AuthenticationType.Password)
                                )
                            )
                        ),
                        getFallbackUrlCallback = mockFunction1(mocker),
                        authenticateCallback = mockFunction1(mocker) {
                            Result.failure(RuntimeException("Oh no!"))
                        },
                        onSuccessCallback = mockFunction0(mocker),
                    )
                )
                everySuspending { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual(ourDeviceId))
                } returns flowOf(DeviceTrustLevel.Valid(true))
                every {
                    keyServiceMock.getTrustLevel(isEqual(ourUserId), isEqual("deviceId2"))
                } returns flowOf(DeviceTrustLevel.NotCrossSigned)
            }

            val cut = devicesSettingsViewModel(coroutineContext)
            val accountsWithDevices = cut.accountsWithDevices
            accountsWithDevices.first { it.isNotEmpty() && it[0].devicesInAccount.value.thisDevice.deviceId == ourDeviceId }

            cut.removeError.value shouldBe null
            cut.remove("test", "deviceId2")
            testCoroutineScheduler.advanceUntilIdle()
            cut.authenticate("test", "p4ssw0rd!", "deviceId2")
            testCoroutineScheduler.advanceUntilIdle()

            cut.removeError.value shouldNotBe null
            cut.closeRemoveDialog()
            cut.removeError.value shouldBe null

            cancelNeverEndingCoroutines()
        }
    }

    private fun activeDeviceVerification(scope: CoroutineScope) = ActiveDeviceVerification(
        request = VerificationRequestEventContent("", emptySet(), 0L, ""),
        requestIsOurs = false,
        ownUserId = ourUserId,
        ownDeviceId = ourDeviceId,
        theirDeviceId = "",
        theirUserId = UserId(""),
        theirDeviceIds = emptySet(),
        supportedMethods = emptySet(),
        api = matrixClientServerApiClientMock,
        olmDecrypter = olmDecrypterMock,
        olmEncryptionService = olmEncryptionServiceMock,
        keyTrust = keyTrustServiceMock,
        keyStore = KeyStore(
            outdatedKeysRepository = InMemoryOutdatedKeysRepository(),
            deviceKeysRepository = InMemoryDeviceKeysRepository(),
            crossSigningKeysRepository = InMemoryCrossSigningKeysRepository(),
            keyVerificationStateRepository = InMemoryKeyVerificationStateRepository(),
            keyChainLinkRepository = InMemoryKeyChainLinkRepository(),
            secretsRepository = InMemorySecretsRepository(),
            secretKeyRequestRepository = InMemorySecretKeyRequestRepository(),
            roomKeyRequestRepository = InMemoryRoomKeyRequestRepository(),
            tm = NoOpRepositoryTransactionManager,
            config = MatrixClientConfiguration(),
            storeScope = scope,
        ),
    )

    private fun devicesSettingsViewModel(coroutineContext: CoroutineContext): DevicesSettingsViewModelImpl {
        val di = koinApplication {
            modules(
                trixnityMessengerModule(),
                testMatrixClientModule(listOf(matrixClientMock, matrixClientMock2), listOf("test", "test2")),
            )
        }.koin
        di.get<I18n>().setCurrentLang("en")
        return DevicesSettingsViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                accountName = "test",
                coroutineContext = coroutineContext
            ),
            mockFunction0(mocker),
        )
    }
}
