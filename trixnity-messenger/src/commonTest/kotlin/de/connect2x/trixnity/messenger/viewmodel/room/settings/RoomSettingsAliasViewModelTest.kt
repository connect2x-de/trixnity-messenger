package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.model.rooms.GetRoomAlias
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds


class RoomSettingsAliasViewModelTest {
    private val roomId = RoomId("room", "127.0.0.1")
    private val me = UserId("user", "127.0.0.1")

    private var matrixClientMock = mock<MatrixClient>()
    private var roomServiceMock = mock<RoomService>()
    private var userServiceMock = mock<UserService>()
    private var matrixClientServerApiMock = mock<MatrixClientServerApiClient>()
    private var roomsApiClientMock = mock<RoomApiClient>()

    private val canSendEvent = MutableStateFlow(true)
    private val serverAliases = MutableStateFlow<CanonicalAliasEventContent?>(null)
    private val directoryAliases = MutableStateFlow(emptyMap<RoomAliasId, RoomId>())

    init {
        serverAliases.value = null
        canSendEvent.value = true
        directoryAliases.value = emptyMap()

        every {
            roomServiceMock.getState(any(), eq(CanonicalAliasEventContent::class), any())
        } returns serverAliases.map {
            if (it == null) null
            else ClientEvent.RoomEvent.StateEvent(
                content = it,
                roomId = roomId,
                stateKey = "",
                id = EventId("\$eventId"),
                sender = me,
                unsigned = null,
                originTimestamp = 1234
            )
        }

        everySuspend {
            roomsApiClientMock.sendStateEvent(any(), any(), any(), any())
        } calls {
            serverAliases.value = it.args[1] as CanonicalAliasEventContent?
            Result.success(EventId("\$eventId"))
        }

        everySuspend {
            roomsApiClientMock.getRoomAlias(any())
        } calls {
            directoryAliases.value[it.args[0]]?.let {
                Result.success(
                    GetRoomAlias.Response(it, listOf("127.0.0.1"))
                )
            } ?: if (MatrixRegex.roomAlias.matches((it.args[0] as RoomAliasId).full)) {
                Result.failure(
                    MatrixServerException(
                        HttpStatusCode.NotFound, ErrorResponse.NotFound("")
                    )
                )
            } else {
                Result.failure(
                    MatrixServerException(
                        HttpStatusCode.BadRequest, ErrorResponse.InvalidParam("")
                    )
                )
            }
        }

        everySuspend {
            roomsApiClientMock.setRoomAlias(any(), any(), any())
        } calls {
            directoryAliases.value += it.args[1] as RoomAliasId to it.args[0] as RoomId
            Result.success(Unit)
        }

        everySuspend {
            roomsApiClientMock.deleteRoomAlias(any(), any())
        } calls {
            directoryAliases.value -= it.args[0] as RoomAliasId
            Result.success(Unit)
        }

        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        every { matrixClientMock.userId } returns me
        every { matrixClientMock.api } returns matrixClientServerApiMock
        every { matrixClientServerApiMock.room } returns roomsApiClientMock

        every {
            userServiceMock.canSendEvent(any(), eq(CanonicalAliasEventContent::class))
        } returns canSendEvent
    }


    @Test
    fun `load permissions to change the room alias based on the user's power level`() = runTest {
        val viewModel = roomSettingsAliasViewModel()

        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe true
        }

        canSendEvent.value = false
        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe false
        }
    }

    @Test
    fun `add alias`() = runTest {
        val error = MutableStateFlow<String?>(null)
        val viewModel = roomSettingsAliasViewModel(addError = error)

        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe true
        }

        viewModel.newAlias.update("#epicroom:127.0.0.1")
        viewModel.addNewAlias()

        eventually(10.seconds) {
            viewModel.isUpdating.value shouldBe false
            error.value shouldBe null
            viewModel.moreAliases.value shouldContain "#epicroom:127.0.0.1"
        }

        verifySuspend(mode = VerifyMode.soft) {
            roomsApiClientMock.sendStateEvent(
                any(), eq(
                    CanonicalAliasEventContent(
                        null, setOf(RoomAliasId("#epicroom:127.0.0.1"))
                    )
                ), any(), any()
            )
        }
    }

    @Test
    fun `not add invalid alias`() = runTest {
        val error = MutableStateFlow<String?>(null)
        val viewModel = roomSettingsAliasViewModel(addError = error)

        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe true
        }

        viewModel.newAlias.update("epicroom:127.0.0.1")
        viewModel.addNewAlias()

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            error.value shouldBe viewModel.i18n.settingsRoomAliasAddAliasInvalid()
            viewModel.moreAliases.value shouldNotContain "epicroom:127.0.0.1"
        }
    }

    @Test
    fun `main alias » set main alias`() = runTest {
        val addError = MutableStateFlow<String?>(null)
        val changeMainAliasError = MutableStateFlow<String?>(null)
        val viewModel = roomSettingsAliasViewModel(
            addError = addError, updateError = changeMainAliasError
        )

        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe true
        }

        viewModel.newAlias.update("#epicroom:127.0.0.1")
        viewModel.addNewAlias()

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            addError.value shouldBe null
            viewModel.moreAliases.value shouldContain "#epicroom:127.0.0.1"
        }

        viewModel.changeMainAlias(RoomAliasId("#epicroom:127.0.0.1"))

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            changeMainAliasError.value shouldBe null
            viewModel.moreAliases.value shouldNotContain "#epicroom:127.0.0.1"
            viewModel.mainAlias.value shouldBe "#epicroom:127.0.0.1"
        }

        verifySuspend(mode = VerifyMode.soft) {
            roomsApiClientMock.sendStateEvent(
                any(), eq(
                    CanonicalAliasEventContent(
                        RoomAliasId("#epicroom:127.0.0.1"), setOf()
                    )
                ), any(), any()
            )
        }
    }

    @Test
    fun `main alias » remove main alias when null is passed`() = runTest {
        val addError = MutableStateFlow<String?>(null)
        val changeMainAliasError = MutableStateFlow<String?>(null)
        val viewModel = roomSettingsAliasViewModel(
            addError = addError, updateError = changeMainAliasError
        )

        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe true
        }

        viewModel.newAlias.update("#epicroom:127.0.0.1")
        viewModel.addNewAlias()

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            addError.value shouldBe null
            viewModel.moreAliases.value shouldContain "#epicroom:127.0.0.1"
        }

        viewModel.changeMainAlias(RoomAliasId("#epicroom:127.0.0.1"))

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            changeMainAliasError.value shouldBe null
            viewModel.moreAliases.value shouldNotContain "#epicroom:127.0.0.1"
            viewModel.mainAlias.value shouldBe "#epicroom:127.0.0.1"
        }

        viewModel.changeMainAlias(null)

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            changeMainAliasError.value shouldBe null
            viewModel.moreAliases.value shouldContain "#epicroom:127.0.0.1"
            viewModel.mainAlias.value shouldBe null
        }

        verifySuspend(mode = VerifyMode.soft) {
            roomsApiClientMock.sendStateEvent(
                any(), eq(
                    CanonicalAliasEventContent(
                        null, setOf(RoomAliasId("#epicroom:127.0.0.1"))
                    )
                ), any(), any()
            )
        }
    }

    @Test
    fun `main alias » don't change main alias when main alias is passed`() = runTest {
        val addError = MutableStateFlow<String?>(null)
        val changeMainAliasError = MutableStateFlow<String?>(null)
        val viewModel = roomSettingsAliasViewModel(
            addError = addError, updateError = changeMainAliasError
        )

        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe true
        }

        viewModel.newAlias.update("#epicroom:127.0.0.1")
        viewModel.addNewAlias()

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            addError.value shouldBe null
            viewModel.moreAliases.value shouldContain "#epicroom:127.0.0.1"
        }

        viewModel.changeMainAlias(RoomAliasId("#epicroom:127.0.0.1"))

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            changeMainAliasError.value shouldBe null
            viewModel.moreAliases.value shouldNotContain "#epicroom:127.0.0.1"
            viewModel.mainAlias.value shouldBe "#epicroom:127.0.0.1"
        }

        viewModel.changeMainAlias(RoomAliasId("#epicroom:127.0.0.1"))

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            changeMainAliasError.value shouldBe null
            viewModel.moreAliases.value shouldNotContain "#epicroom:127.0.0.1"
            viewModel.mainAlias.value shouldBe "#epicroom:127.0.0.1"
        }
    }

    @Test
    fun `main alias » set main alias and degrade original main alias`() = runTest {
        val addError = MutableStateFlow<String?>(null)
        val changeMainAliasError = MutableStateFlow<String?>(null)
        val viewModel = roomSettingsAliasViewModel(
            addError = addError, updateError = changeMainAliasError
        )

        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe true
        }

        viewModel.newAlias.update("#epicroom:127.0.0.1")
        viewModel.addNewAlias()

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            addError.value shouldBe null
            viewModel.moreAliases.value shouldContain "#epicroom:127.0.0.1"
        }

        viewModel.newAlias.update("#awesomeroom:127.0.0.1")
        viewModel.addNewAlias()

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            addError.value shouldBe null
            viewModel.moreAliases.value shouldContain "#awesomeroom:127.0.0.1"
        }

        viewModel.changeMainAlias(RoomAliasId("#epicroom:127.0.0.1"))

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            changeMainAliasError.value shouldBe null
            viewModel.moreAliases.value shouldNotContain "#epicroom:127.0.0.1"
            viewModel.mainAlias.value shouldBe "#epicroom:127.0.0.1"
        }

        viewModel.changeMainAlias(RoomAliasId("#awesomeroom:127.0.0.1"))

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            changeMainAliasError.value shouldBe null
            viewModel.moreAliases.value shouldContain "#epicroom:127.0.0.1"
            viewModel.moreAliases.value shouldNotContain "#awesomeroom:127.0.0.1"
            viewModel.mainAlias.value shouldBe "#awesomeroom:127.0.0.1"
        }

        verifySuspend(mode = VerifyMode.soft) {
            roomsApiClientMock.sendStateEvent(
                any(), eq(
                    CanonicalAliasEventContent(
                        RoomAliasId("#awesomeroom:127.0.0.1"), setOf(RoomAliasId("#epicroom:127.0.0.1"))
                    )
                ), any(), any()
            )
        }
    }

    @Test
    fun `not set unrelated alias`() = runTest {
        val error = MutableStateFlow<String?>(null)
        val viewModel = roomSettingsAliasViewModel(updateError = error)

        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe true
        }

        viewModel.changeMainAlias(RoomAliasId("epicroom:127.0.0.1"))

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            error.value shouldBe viewModel.i18n.settingsRoomAliasChangeMainUnrelatedAlias()
        }
    }

    @Test
    fun `remove common alias`() = runTest {
        val addError = MutableStateFlow<String?>(null)
        val removeAliasError = MutableStateFlow<String?>(null)
        val viewModel = roomSettingsAliasViewModel(addError = addError, removeError = removeAliasError)

        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe true
        }

        viewModel.newAlias.update("#epicroom:127.0.0.1")
        viewModel.addNewAlias()

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            addError.value shouldBe null
            viewModel.moreAliases.value shouldContain "#epicroom:127.0.0.1"
        }

        viewModel.removeAlias(RoomAliasId("#epicroom:127.0.0.1"))

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            removeAliasError.value shouldBe null
            viewModel.moreAliases.value shouldNotContain "#epicroom:127.0.0.1"
        }

        verifySuspend(mode = VerifyMode.soft) {
            roomsApiClientMock.sendStateEvent(
                any(), eq(
                    CanonicalAliasEventContent(
                        null, setOf()
                    )
                ), any(), any()
            )
        }
    }

    @Test
    fun `remove main alias`() = runTest {
        val addError = MutableStateFlow<String?>(null)
        val changeMainAliasError = MutableStateFlow<String?>(null)
        val removeAliasError = MutableStateFlow<String?>(null)
        val viewModel = roomSettingsAliasViewModel(
            addError = addError, updateError = changeMainAliasError, removeError = removeAliasError
        )

        eventually(2.seconds) {
            viewModel.canChangeRoomAlias.value shouldBe true
        }

        viewModel.newAlias.update("#epicroom:127.0.0.1")
        viewModel.addNewAlias()

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            addError.value shouldBe null
            viewModel.moreAliases.value shouldContain "#epicroom:127.0.0.1"
        }

        viewModel.changeMainAlias(RoomAliasId("#epicroom:127.0.0.1"))

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            changeMainAliasError.value shouldBe null
            viewModel.moreAliases.value shouldNotContain "#epicroom:127.0.0.1"
            viewModel.mainAlias.value shouldBe "#epicroom:127.0.0.1"
        }

        viewModel.removeAlias(RoomAliasId("#epicroom:127.0.0.1"))

        eventually(2.seconds) {
            viewModel.isUpdating.value shouldBe false
            removeAliasError.value shouldBe null
            viewModel.mainAlias.value shouldNotBe "#epicroom:127.0.0.1"
        }

        verifySuspend(mode = VerifyMode.soft) {
            roomsApiClientMock.sendStateEvent(
                any(), eq(
                    CanonicalAliasEventContent(
                        null, setOf()
                    )
                ), any(), any()
            )
        }
    }

    private fun TestScope.roomSettingsAliasViewModel(
        removeError: MutableStateFlow<String?> = MutableStateFlow(null),
        updateError: MutableStateFlow<String?> = MutableStateFlow(null),
        addError: MutableStateFlow<String?> = MutableStateFlow(null),
    ): RoomSettingsAliasViewModelImpl {
        return RoomSettingsAliasViewModelImpl(
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
            selectedRoomId = roomId,
            isDirect = MutableStateFlow(false),
            removeAliasError = removeError,
            updateError = updateError,
            newAliasError = addError,
        )
    }
}
