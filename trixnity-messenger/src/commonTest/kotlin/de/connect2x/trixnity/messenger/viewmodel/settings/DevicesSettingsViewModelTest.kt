package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.firstNotNullWithClue
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaImpl
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.SuspendAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.KeyTrustService
import net.folivo.trixnity.client.store.KeyStore
import net.folivo.trixnity.client.store.cache.ObservableCacheStatisticCollector
import net.folivo.trixnity.client.store.repository.InMemoryCrossSigningKeysRepository
import net.folivo.trixnity.client.store.repository.InMemoryDeviceKeysRepository
import net.folivo.trixnity.client.store.repository.InMemoryKeyChainLinkRepository
import net.folivo.trixnity.client.store.repository.InMemoryKeyVerificationStateRepository
import net.folivo.trixnity.client.store.repository.InMemoryOutdatedKeysRepository
import net.folivo.trixnity.client.store.repository.InMemoryRoomKeyRequestRepository
import net.folivo.trixnity.client.store.repository.InMemorySecretKeyRequestRepository
import net.folivo.trixnity.client.store.repository.InMemorySecretsRepository
import net.folivo.trixnity.client.store.repository.NoOpRepositoryTransactionManager
import net.folivo.trixnity.client.verification.ActiveDeviceVerificationImpl
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.clientserverapi.client.DeviceApiClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.devices.Device
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequestToDeviceEventContent
import net.folivo.trixnity.core.model.keys.DeviceKeys
import net.folivo.trixnity.core.model.keys.Keys
import net.folivo.trixnity.crypto.key.DeviceTrustLevel
import net.folivo.trixnity.crypto.olm.OlmDecrypter
import net.folivo.trixnity.crypto.olm.OlmEncryptionService
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class DevicesSettingsViewModelTest {
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

    val matrixClientMock = mock<MatrixClient>()

    val matrixClientMock2 = mock<MatrixClient>()

    val keyServiceMock = mock<KeyService>()

    val keyServiceMock2 = mock<KeyService>()

    val verificationServiceMock = mock<VerificationService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val matrixClientServerApiClientMock2 = mock<MatrixClientServerApiClient>()

    val devicesApiClientMock = mock<DeviceApiClient>()

    val devicesApiClientMock2 = mock<DeviceApiClient>()

    val olmDecrypterMock = mock<OlmDecrypter>()

    val olmEncryptionServiceMock = mock<OlmEncryptionService>()

    val keyTrustServiceMock = mock<KeyTrustService>()

    val clock = mock<Clock>()

    private var authorizeUia: AuthorizeUia = AuthorizeUiaImpl()

    private var updateDeviceMocker: SuspendAnsweringScope<Result<Unit>>
    private var deviceKeysMocker: BlockingAnsweringScope<Flow<List<DeviceKeys>?>>

    init {
        resetMocks(
            matrixClientMock,
            matrixClientMock2,
            keyServiceMock,
            keyServiceMock2,
            verificationServiceMock,
            matrixClientServerApiClientMock,
            matrixClientServerApiClientMock2,
            devicesApiClientMock,
            devicesApiClientMock2,
            olmDecrypterMock,
            olmEncryptionServiceMock,
            keyTrustServiceMock
        )

        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { keyServiceMock }
                    single { verificationServiceMock }
                })
        }.koin
        every { matrixClientMock.deviceId } returns ourDeviceId
        every { matrixClientMock.userId } returns ourUserId
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.device } returns devicesApiClientMock
        every { matrixClientServerApiClientMock.json } returns Json

        every { matrixClientMock2.di } returns koinApplication {
            modules(
                module {
                    single { keyServiceMock2 }
                    single { verificationServiceMock }
                })
        }.koin
        every { matrixClientMock2.deviceId } returns ourDeviceId2
        every { matrixClientMock2.userId } returns ourUserId2
        every { matrixClientMock2.api } returns matrixClientServerApiClientMock2
        every { matrixClientServerApiClientMock2.device } returns devicesApiClientMock2
        every { matrixClientServerApiClientMock2.json } returns Json

        updateDeviceMocker = everySuspend {
            devicesApiClientMock.updateDevice(any(), any(), eqNull())
        }
        updateDeviceMocker returns Result.success(Unit)

        deviceKeysMocker = every { keyServiceMock.getDeviceKeys(eq(ourUserId)) }
        deviceKeysMocker returns deviceKeys
        every { keyServiceMock2.getDeviceKeys(eq(ourUserId2)) } returns deviceKeys2
        // standard values for mock2, we do not need any config here
        every {
            keyServiceMock2.getTrustLevel(any<UserId>(), any())
        } returns flowOf(DeviceTrustLevel.Valid(true))
        everySuspend { devicesApiClientMock2.getDevices() } returns Result.success(
            listOf(device21, device22)
        )
        everySuspend {
            devicesApiClientMock.getDevice(any(), any())
        } returns Result.success(device1)
    }

    @Test
    fun `load devices initially`() = runTest {
        everySuspend { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq(ourDeviceId))
        } returns flowOf(DeviceTrustLevel.Valid(true))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq("deviceId2"))
        } returns flowOf(DeviceTrustLevel.NotCrossSigned)

        val cut = devicesSettingsViewModel()
        val accountsWithDevices = cut.accountsWithDevices
        accountsWithDevices.first { it.size == 2 }

        eventually(2.seconds) {
            assertSoftly(accountsWithDevices.value) {
                get(0).userId shouldBe UserId("test", "server")
                get(1).userId shouldBe UserId("test2", "server")

                get(0).isLoading.first { it.not() }
                get(0).devicesInAccount.filterNotNull().first { it.thisDevice.deviceId == ourDeviceId }
                assertSoftly(get(0).devicesInAccount.value.shouldNotBeNull().thisDevice) {
                    deviceId shouldBe ourDeviceId
                    displayName.value shouldBe "device1"
                    lastSeenAt shouldBe "last seen: 12/10/2021"
                    isVerified.value shouldBe true
                }
                get(0).devicesInAccount.value.shouldNotBeNull().otherDevices shouldHaveSize 1
                assertSoftly(get(0).devicesInAccount.value.shouldNotBeNull().otherDevices[0]) {
                    deviceId shouldBe "deviceId2"
                    displayName.value shouldBe "device2"
                    lastSeenAt shouldBe "last seen: 12/10/2021"
                    isVerified.value shouldBe false
                }

                get(1).devicesInAccount.filterNotNull().first { it.thisDevice.deviceId == ourDeviceId2 }
                get(1).devicesInAccount.value.shouldNotBeNull().thisDevice.deviceId shouldBe ourDeviceId2
                get(1).devicesInAccount.value.shouldNotBeNull().otherDevices shouldHaveSize 1
            }
        }
    }

    @Test
    fun `react to changes in the trust level of devices`() = runTest {
        val trustLevel1 = MutableStateFlow<DeviceTrustLevel>(DeviceTrustLevel.CrossSigned(true))
        val trustLevel2 = MutableStateFlow<DeviceTrustLevel>(DeviceTrustLevel.NotCrossSigned)
        everySuspend { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq(ourDeviceId))
        } returns trustLevel1
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq("deviceId2"))
        } returns trustLevel2

        val cut = devicesSettingsViewModel()
        val accountsWithDevices = cut.accountsWithDevices

        trustLevel1.value = DeviceTrustLevel.CrossSigned(false)
        trustLevel2.value = DeviceTrustLevel.CrossSigned(true)

        accountsWithDevices.first { it.size == 2 }

        eventually(2.seconds) {
            assertSoftly(accountsWithDevices.value) {
                get(0).devicesInAccount.filterNotNull().first { it.thisDevice.deviceId == ourDeviceId }
                assertSoftly(get(0).devicesInAccount.value.shouldNotBeNull()) {
                    thisDevice.isVerified.value shouldBe false
                    otherDevices[0].isVerified.value shouldBe true
                }
            }
        }
    }

    @Test
    fun `react to changes in the devices list`() = runTest {
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

        val getDevicesMocker = everySuspend { devicesApiClientMock.getDevices() }
        getDevicesMocker returns Result.success(listOf(device1, device2))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq(ourDeviceId))
        } returns flowOf(DeviceTrustLevel.Valid(true))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq("deviceId2"))
        } returns flowOf(DeviceTrustLevel.NotCrossSigned)

        val cut = devicesSettingsViewModel()
        val accountsWithDevices = cut.accountsWithDevices

        eventually(2.seconds) {
            accountsWithDevices.value shouldNot beEmpty()
            accountsWithDevices.value[0].devicesInAccount.value.shouldNotBeNull().otherDevices shouldNot beEmpty()
            accountsWithDevices.value[0].devicesInAccount.value.shouldNotBeNull().otherDevices[0].displayName.value shouldBe "device2"
        }

        getDevicesMocker returns Result.success(
            listOf(
                device1, device2.copy(displayName = "device2___new")
            )
        )

        deviceKeysList.value += DeviceKeys(ourUserId, ourDeviceId, setOf(), Keys(setOf()))
        eventually(2.seconds) {
            accountsWithDevices.value shouldNot beEmpty()
            accountsWithDevices.value[0].devicesInAccount.value.shouldNotBeNull().otherDevices shouldNot beEmpty()
            accountsWithDevices.value[0].devicesInAccount.value.shouldNotBeNull().otherDevices[0].displayName.value shouldBe "device2___new"
        }
    }


    @Test
    fun `show an error message if loading devices cannot be performed`() = runTest {
        everySuspend { devicesApiClientMock.getDevices() } returns Result.failure(RuntimeException("Oh no!"))

        val cut = devicesSettingsViewModel()
        val accountsWithDevices = cut.accountsWithDevices

        @OptIn(ExperimentalCoroutinesApi::class)
        accountsWithDevices.filter { it.isNotEmpty() }.flatMapLatest { combine(it.map { it.loadingError }) { it } }
            .first {
                it[0] != null
            }
    }

    @Test
    fun `set the display name for this device and update the device`() = runTest {
        val device = MutableStateFlow(device1)

        everySuspend {
            devicesApiClientMock.updateDevice(any(), any(), eqNull())
        } calls {
            val deviceId = it.args[0] as String
            val deviceName = it.args[1] as String

            if (deviceId == device.value.deviceId) {
                device.value = device.value.copy(displayName = deviceName)
            }

            Result.success(Unit)
        }
        everySuspend { devicesApiClientMock.getDevices() } calls { Result.success(listOf(device.value, device2)) }

        every {
            keyServiceMock.getTrustLevel(any<UserId>(), any())
        } returns flowOf(DeviceTrustLevel.Valid(true))

        val cut = devicesSettingsViewModel()
        // wait until initial computation is done
        val accountsWithDevices = cut.accountsWithDevices
        eventually(1.seconds) {
            accountsWithDevices.filter { it.isNotEmpty() }
                .first { it[0].devicesInAccount.value.shouldNotBeNull().thisDevice.deviceId == ourDeviceId }
        }

        cut.error.value shouldBe null
        cut.setDisplayName(UserId("test", "server"), ourDeviceId, "device1", "device1 updated")

        eventually(1.seconds) {
            cut.error.value shouldBe null
            accountsWithDevices.filter { it.isNotEmpty() }.first {
                it[0].devicesInAccount.value.shouldNotBeNull().thisDevice.displayName.value == "device1 updated"
            }
        }
    }

    @Test
    fun `display error message when device cannot be renamed`() = runTest {
        updateDeviceMocker returns Result.failure(RuntimeException("Oh no!"))
        everySuspend { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
        every {
            keyServiceMock.getTrustLevel(any<UserId>(), any())
        } returns flowOf(DeviceTrustLevel.Valid(true))

        val cut = devicesSettingsViewModel()
        val accountsWithDevices = cut.accountsWithDevices
        eventually(1.seconds) {
            accountsWithDevices.filter { it.isNotEmpty() }
                .first { it[0].devicesInAccount.value.shouldNotBeNull().thisDevice.deviceId == ourDeviceId }
        }

        cut.error.value shouldBe null
        cut.setDisplayName(UserId("test", "server"), ourDeviceId, "device1", "device1 updated")

        eventually(1.seconds) {
            cut.error.value shouldNotBe null
        }
    }

    @Test
    fun `initiate a verification request`() = runTest {
        everySuspend {
            verificationServiceMock.createDeviceVerificationRequest(eq(ourUserId), any())
        } returns Result.success(activeDeviceVerification())
        everySuspend { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq(ourDeviceId))
        } returns flowOf(DeviceTrustLevel.Valid(true))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq("deviceId2"))
        } returns flowOf(DeviceTrustLevel.NotCrossSigned)

        val cut = devicesSettingsViewModel()
        val accountsWithDevices = cut.accountsWithDevices
        eventually(1.seconds) {
            accountsWithDevices.filter { it.isNotEmpty() }
                .first { it[0].devicesInAccount.value.shouldNotBeNull().thisDevice.deviceId == ourDeviceId }
        }

        cut.verify(UserId("test", "server"), "deviceId2")

        eventually(1.seconds) {
            verifySuspend {
                verificationServiceMock.createDeviceVerificationRequest(
                    eq(ourUserId),
                    eq(setOf("deviceId2")),
                )
            }
        }
    }

    @Test
    fun `show an error message when verification cannot be performed`() = runTest {
        everySuspend {
            verificationServiceMock.createDeviceVerificationRequest(eq(ourUserId), any())
        } calls { throw RuntimeException("Oh no!") }
        everySuspend { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq(ourDeviceId))
        } returns flowOf(DeviceTrustLevel.Valid(true))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq("deviceId2"))
        } returns flowOf(DeviceTrustLevel.NotCrossSigned)

        val cut = devicesSettingsViewModel()
        val accountsWithDevices = cut.accountsWithDevices
        eventually(1.seconds) {
            accountsWithDevices.filter { it.isNotEmpty() }
                .first { it[0].devicesInAccount.value.shouldNotBeNull().thisDevice.deviceId == ourDeviceId }
        }
        cut.error.value shouldBe null
        cut.verify(UserId("test", "server"), "deviceId2")

        eventually(1.seconds) {
            cut.error.value shouldNotBe null
        }
    }

    @Test
    fun `show uia when trying to remove a device`() = runTest {
        everySuspend { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq(ourDeviceId))
        } returns flowOf(DeviceTrustLevel.Valid(true))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq("deviceId2"))
        } returns flowOf(DeviceTrustLevel.NotCrossSigned)

        val cut = devicesSettingsViewModel()
        val accountsWithDevices = cut.accountsWithDevices
        eventually(1.seconds) {
            accountsWithDevices.filter { it.isNotEmpty() }
                .first { it[0].devicesInAccount.value.shouldNotBeNull().thisDevice.deviceId == ourDeviceId }
        }

        cut.remove(UserId("test", "server"), "deviceId2")
        val authorizeUiaParams = authorizeUia.onRequestFlow.firstNotNullWithClue()
        authorizeUiaParams.onResult(AuthorizeUiaResult.Success(UIA.Success(Unit)))
    }

    @Test
    fun `show an error message when the device could not be removed`() = runTest {
        everySuspend { devicesApiClientMock.getDevices() } returns Result.success(listOf(device1, device2))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq(ourDeviceId))
        } returns flowOf(DeviceTrustLevel.Valid(true))
        every {
            keyServiceMock.getTrustLevel(eq(ourUserId), eq("deviceId2"))
        } returns flowOf(DeviceTrustLevel.NotCrossSigned)

        val cut = devicesSettingsViewModel()
        val accountsWithDevices = cut.accountsWithDevices
        eventually(1.seconds) {
            accountsWithDevices.filter { it.isNotEmpty() }
                .first { it[0].devicesInAccount.value.shouldNotBeNull().thisDevice.deviceId == ourDeviceId }
        }

        cut.error.value shouldBe null
        cut.remove(UserId("test", "server"), "deviceId2")
        val authorizeUiaParams = authorizeUia.onRequestFlow.firstNotNullWithClue()
        authorizeUiaParams.onResult(AuthorizeUiaResult.CancelledByUser<Unit>("cancelled"))

        cut.error.firstNotNullWithClue()
    }

    private fun TestScope.activeDeviceVerification() = ActiveDeviceVerificationImpl(
        request = VerificationRequestToDeviceEventContent("", emptySet(), 0L, ""),
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
            statisticCollector = ObservableCacheStatisticCollector(),
            storeScope = backgroundScope,
            clock = Clock.System,
        ),
        clock = clock,
    )

    private fun TestScope.devicesSettingsViewModel(): DevicesSettingsViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(
                        UserId("test", "server") to matrixClientMock, UserId("test2", "server") to matrixClientMock2
                    )
                ) + module {
                    single<AuthorizeUia> { authorizeUia }
                })
        }.koin
        return DevicesSettingsViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = UserId("test", "server"),
            ),
            mock(),
        ).also {
            it.subscribeList()
        }
    }

    private fun DevicesSettingsViewModelImpl.subscribeList() {
        coroutineScope.launch {
            accountsWithDevices.scopedCollectLatest {
                it.forEach { launch { it.devicesInAccount.collect {} } }
            }
        }
    }
}
