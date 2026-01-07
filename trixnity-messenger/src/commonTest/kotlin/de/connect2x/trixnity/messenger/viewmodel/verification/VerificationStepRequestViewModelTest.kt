package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.DeviceApiClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.clientserverapi.model.devices.Device
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds


class VerificationStepRequestViewModelTest {

    private val ourUserId = UserId("me", "server")
    private val ourDeviceId = "device1"
    private val ourUserDisplayName = "Alice"
    private val ourDeviceDisplayName = "MyDevice"
    private val ourDevice = Device(
        ourDeviceId,
        ourDeviceDisplayName,
        "1.2.3.4",
        LocalDateTime.parse("2021-12-10T09:50:00.00").toInstant(TimeZone.UTC).toEpochMilliseconds(),
    )

    private val theirUserId = UserId("them", "server")
    private val theirDeviceId = "device21"
    private val theirUserDisplayName = "Bob"
    private val theirDeviceDisplayName = "TheirDevice"
    private val theirDevice = Device(
        theirDeviceId,
        theirDeviceDisplayName,
        "4.3.2.1",
        LocalDateTime.parse("2021-12-10T07:50:00.00").toInstant(TimeZone.UTC).toEpochMilliseconds(),
    )

    val matrixClientMock = mock<MatrixClient>()
    val usersApiClientMock = mock<UserApiClient>()
    val devicesApiClientMock = mock<DeviceApiClient>()
    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    init {
        every { matrixClientMock.di } returns koinApplication { modules() }.koin
        every { matrixClientMock.userId } returns ourUserId
        every { matrixClientMock.deviceId } returns ourDeviceId
        every { matrixClientMock.displayName } returns MutableStateFlow(ourUserDisplayName)
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.device } returns devicesApiClientMock
        every { matrixClientServerApiClientMock.user } returns usersApiClientMock
        every { matrixClientMock.displayName } returns MutableStateFlow(ourUserDisplayName)
        everySuspend { usersApiClientMock.getDisplayName(theirUserId) } returns Result.success(theirUserDisplayName)
        everySuspend { devicesApiClientMock.getDevice(ourDeviceId, any()) } returns Result.success(ourDevice)
        everySuspend { devicesApiClientMock.getDevice(theirDeviceId, any()) } returns Result.success(theirDevice)
    }

    @Test
    fun `get own user data`() = runTest {
        val cut = verificationStepRequestViewModel()
        backgroundScope.launch { cut.ourDisplayName.collect() }
        backgroundScope.launch { cut.ourDeviceDisplayName.collect() }
        eventually(1.seconds) {
            cut.ourUserId shouldBe ourUserId
            cut.ourDisplayName.value shouldBe ourUserDisplayName
            cut.ourDeviceDisplayName.value shouldBe ourDeviceDisplayName
        }
    }

    @Test
    fun `get external user data`() = runTest {
        val cut = verificationStepRequestViewModel()
        backgroundScope.launch { cut.theirDisplayName.collect() }
        backgroundScope.launch { cut.theirDeviceDisplayName.collect() }
        eventually(1.seconds) {
            cut.theirUserId shouldBe theirUserId
            cut.theirDisplayName.value shouldBe theirUserDisplayName
            cut.theirDeviceDisplayName.value shouldBe theirDeviceDisplayName
        }
    }

    @Test
    fun `return default when fetching external user display name on denied access`() = runTest {
        val cut = verificationStepRequestViewModel()
        everySuspend { usersApiClientMock.getDisplayName(theirUserId) } returns responseForbidden()
        backgroundScope.launch { cut.theirDisplayName.collect() }
        eventually(1.seconds) {
            cut.theirDisplayName.value shouldBe theirUserId.full
        }
    }

    @Test
    fun `return default when fetching own device display name on denied access`() = runTest {
        val cut = verificationStepRequestViewModel()
        everySuspend { devicesApiClientMock.getDevice(ourDeviceId, any()) } returns responseForbidden()
        backgroundScope.launch { cut.ourDeviceDisplayName.collect() }
        eventually(1.seconds) {
            cut.ourDeviceDisplayName.value shouldBe ourDeviceId
        }
    }

    @Test
    fun `return default when fetching external device display name on denied access`() = runTest {
        val cut = verificationStepRequestViewModel()
        everySuspend { devicesApiClientMock.getDevice(theirDeviceId, any()) } returns responseForbidden()
        backgroundScope.launch { cut.theirDeviceDisplayName.collect() }
        eventually(1.seconds) {
            cut.theirDeviceDisplayName.value shouldBe theirDeviceId
        }
    }

    @Test
    fun `recognize if request is from the same account`() = runTest {
        val senderId = ourUserId.copy()
        val senderDeviceId = ourDevice.copy().deviceId
        val cut = verificationStepRequestViewModel(senderId, senderDeviceId)
        eventually(1.seconds) {
            cut.isFromOwnAccount shouldBe true
        }
    }

    @Test
    fun `recognize if request is from a different account`() = runTest {
        val senderId = theirUserId.copy()
        val senderDeviceId = theirDevice.copy().deviceId
        val cut = verificationStepRequestViewModel(senderId, senderDeviceId)
        eventually(1.seconds) {
            cut.isFromOwnAccount shouldBe false
        }
    }

    private fun <T> responseForbidden(): Result<T> = Result.failure(
        MatrixServerException(
            HttpStatusCode.Forbidden,
            ErrorResponse.Forbidden("403"),
        )
    )

    private fun TestScope.verificationStepRequestViewModel(
        senderUserId: UserId = theirUserId,
        senderDeviceId: String = theirDeviceId,
    ): VerificationStepRequestViewModel {
        return VerificationStepRequestViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(ourUserId to matrixClientMock)
                        )
                    )
                }.koin,
                userId = ourUserId,
            ),
            {}, senderUserId, senderDeviceId,
        )
    }
}
