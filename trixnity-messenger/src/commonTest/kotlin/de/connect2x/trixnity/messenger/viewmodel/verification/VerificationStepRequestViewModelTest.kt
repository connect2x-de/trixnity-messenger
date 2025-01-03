package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
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
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalCoroutinesApi::class)
class VerificationStepRequestViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private val ourUserId = UserId("me", "server")
    private val ourDeviceId = "device1"
    private val ourUserDisplayName = "Alice"
    private val ourDeviceDisplayName = "MyDevice"
    private val ourDevice = Device(
        ourDeviceId,
        ourDeviceDisplayName,
        "1.2.3.4",
        LocalDateTime.parse("2021-12-10T09:50:00.00")
            .toInstant(TimeZone.UTC).toEpochMilliseconds(),
    )

    private val theirUserId = UserId("them", "server")
    private val theirDeviceId = "device21"
    private val theirUserDisplayName = "Bob"
    private val theirDeviceDisplayName = "TheirDevice"
    private val theirDevice = Device(
        theirDeviceId,
        theirDeviceDisplayName,
        "4.3.2.1",
        LocalDateTime.parse("2021-12-10T07:50:00.00")
            .toInstant(TimeZone.UTC).toEpochMilliseconds(),
    )

    val matrixClientMock = mock<MatrixClient>()
    val usersApiClientMock = mock<UserApiClient>()
    val devicesApiClientMock = mock<DeviceApiClient>()
    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    init {
        isolationMode = IsolationMode.InstancePerTest
        beforeTest {
            Dispatchers.setMain(Dispatchers.Unconfined)
            resetMocks(
                matrixClientMock,
                matrixClientServerApiClientMock,
                devicesApiClientMock,
                usersApiClientMock,
            )
            every { matrixClientMock.di } returns koinApplication { modules() }.koin
            every { matrixClientMock.userId } returns ourUserId
            every { matrixClientMock.deviceId } returns ourDeviceId
            every { matrixClientMock.displayName } returns MutableStateFlow(ourUserDisplayName)
            every { matrixClientMock.api } returns matrixClientServerApiClientMock
            every { matrixClientServerApiClientMock.device } returns devicesApiClientMock
            every { matrixClientServerApiClientMock.user } returns usersApiClientMock
            every { matrixClientMock.displayName } returns MutableStateFlow(ourUserDisplayName)
            everySuspend { usersApiClientMock.getDisplayName(eq(theirUserId)) } returns
                    Result.success(theirUserDisplayName)
            everySuspend { devicesApiClientMock.getDevice(eq(ourDeviceId), any()) } returns
                    Result.success(ourDevice)
            everySuspend { devicesApiClientMock.getDevice(eq(theirDeviceId), any()) } returns
                    Result.success(theirDevice)
        }

        should("get own user data") {
            val cut = verificationStepRequestViewModel()
            launch { cut.ourDisplayName.collect() }
            launch { cut.ourDeviceDisplayName.collect() }
            eventually(1.seconds) {
                cut.ourUserId shouldBe ourUserId
                cut.ourDisplayName.value shouldBe ourUserDisplayName
                cut.ourDeviceDisplayName.value shouldBe ourDeviceDisplayName
            }
            cancelNeverEndingCoroutines()
        }

        should("get external user data") {
            val cut = verificationStepRequestViewModel()
            launch { cut.theirDisplayName.collect() }
            launch { cut.theirDeviceDisplayName.collect() }
            eventually(1.seconds) {
                cut.theirUserId shouldBe theirUserId
                cut.theirDisplayName.value shouldBe theirUserDisplayName
                cut.theirDeviceDisplayName.value shouldBe theirDeviceDisplayName
            }
            cancelNeverEndingCoroutines()
        }

        should("return default when fetching external user display name on denied access") {
            val cut = verificationStepRequestViewModel()
            everySuspend { usersApiClientMock.getDisplayName(eq(theirUserId)) } returns
                    responseForbidden()
            launch { cut.theirDisplayName.collect() }
            eventually(1.seconds) {
                cut.theirDisplayName.value shouldBe theirUserId.full
            }
            cancelNeverEndingCoroutines()
        }

        should("return default when fetching own device display name on denied access") {
            val cut = verificationStepRequestViewModel()
            everySuspend { devicesApiClientMock.getDevice(eq(ourDeviceId), any()) } returns
                    responseForbidden()
            launch { cut.ourDeviceDisplayName.collect() }
            eventually(1.seconds) {
                cut.ourDeviceDisplayName.value shouldBe ourDeviceId
            }
            cancelNeverEndingCoroutines()
        }

        should("return default when fetching external device display name on denied access") {
            val cut = verificationStepRequestViewModel()
            everySuspend { devicesApiClientMock.getDevice(eq(theirDeviceId), any()) } returns
                    responseForbidden()
            launch { cut.theirDeviceDisplayName.collect() }
            eventually(1.seconds) {
                cut.theirDeviceDisplayName.value shouldBe theirDeviceId
            }
            cancelNeverEndingCoroutines()
        }

        should("recognize if request is from the same account") {
            val senderId = ourUserId.copy()
            val senderDeviceId = ourDevice.copy().deviceId
            val cut = verificationStepRequestViewModel(senderId, senderDeviceId)
            eventually(1.seconds) {
                cut.isFromOwnAccount shouldBe true
            }
            cancelNeverEndingCoroutines()
        }

        should("recognize if request is from a different account") {
            val senderId = theirUserId.copy()
            val senderDeviceId = theirDevice.copy().deviceId
            val cut = verificationStepRequestViewModel(senderId, senderDeviceId)
            eventually(1.seconds) {
                cut.isFromOwnAccount shouldBe false
            }
            cancelNeverEndingCoroutines()
        }
    }

    private fun <T> responseForbidden(): Result<T> = Result.failure(
        MatrixServerException(
            HttpStatusCode.Forbidden,
            ErrorResponse.Forbidden("403"),
        )
    )

    private fun verificationStepRequestViewModel(
        senderUserId: UserId = theirUserId,
        senderDeviceId: String = theirDeviceId,
    ): VerificationStepRequestViewModel {
        return VerificationStepRequestViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
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
