package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.model.rooms.GetPublicRoomsResponse
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class SearchGroupViewModelTest {
    private val roomId = RoomId("!room1")
    private val room = Room(roomId)

    private val matrixClientMock = mock<MatrixClient>()
    private val matrixApiClientMock = mock<MatrixClientServerApiClient>()
    private val roomApiClientMock = mock<RoomApiClient>()
    private val roomServiceMock = mock<RoomService>()
    private val i18n = object : I18n(
        DefaultLanguages,
        createTestMatrixMessengerSettingsHolder(),
        GetSystemLang { "en" },
        TimeZone.of("CET"),
    ) {}
    val onGroupJoinedMock: (UserId, RoomId) -> Unit = mock()
    var joined = false
    val onGroupKnockedMock: (RoomId) -> Unit = mock()
    var knocked = false

    @BeforeTest
    fun setup() {
        resetMocks(matrixClientMock, matrixApiClientMock, roomApiClientMock, roomServiceMock)

        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single<I18n> { i18n }
                }
            )
        }.koin

        every { matrixClientMock.api } returns matrixApiClientMock
        every { matrixApiClientMock.room } returns roomApiClientMock
        every { onGroupJoinedMock.invoke(any(), any()) } calls {
            joined = true
        }
        joined = false
        every { onGroupKnockedMock.invoke(any()) } calls {
            knocked = true
        }
        knocked = false

        everySuspend { roomServiceMock.getById(eq(roomId)) } returns flowOf(room)
    }

    @Test
    fun `entering groups - should handle successful joining`() = runTest {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.success(roomId)

        val cut = searchGroupViewModel()

        listOf(JoinRule.Public, JoinRule.Invite, JoinRule.Restricted, JoinRule.KnockRestricted).forEach { rule ->
            everySuspend { roomApiClientMock.getPublicRooms(any(), any(), any(), any(), any(), any(), any()) } returns
                    Result.success(getPublicRoomsResponse(rule))
            loadData(cut)

            joined = false
            cut.enterGroup(roomId)
            delay(500.milliseconds)
            joined shouldBe true
            knocked shouldBe false
        }
    }

    @Test
    fun `entering groups - should handle successful knocking`() = runTest {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns
                Result.success(roomId)

        val cut = searchGroupViewModel()

        listOf(JoinRule.Knock, JoinRule.KnockRestricted).forEach { rule ->
            everySuspend { roomApiClientMock.getPublicRooms(any(), any(), any(), any(), any(), any(), any()) } returns
                    Result.success(getPublicRoomsResponse(rule))
            loadData(cut)

            knocked = false
            cut.enterGroup(roomId)
            delay(500.milliseconds)
            joined shouldBe false
            knocked shouldBe true
        }
    }

    @Test
    fun `entering groups - should handle failure`() = runTest {
        everySuspend { roomApiClientMock.getPublicRooms(any(), any(), any(), any(), any(), any(), any()) } returns
                Result.success(getPublicRoomsResponse(JoinRule.Public))
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))

        val cut = searchGroupViewModel()
        loadData(cut)

        cut.enterGroup(roomId)
        delay(500.milliseconds)
        cut.error.value shouldBe i18n.enterRoomFailedGenericJoin()
    }

    @Test
    fun `entering groups - should handle error`() = runTest {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(Throwable("something went wrong :("))
        everySuspend { roomApiClientMock.getPublicRooms(any(), any(), any(), any(), any(), any(), any()) } returns
                Result.success(getPublicRoomsResponse(JoinRule.Public))

        val cut = searchGroupViewModel()
        loadData(cut)

        cut.enterGroup(roomId)
        delay(500.milliseconds)
        cut.error.value shouldBe i18n.enterRoomFailedGenericJoin()
    }

    private fun getPublicRoomsResponse(joinRule: JoinRule) =
        GetPublicRoomsResponse(
            listOf(
                GetPublicRoomsResponse.PublicRoomsChunk(
                    guestCanJoin = false,
                    joinedMembersCount = 1L,
                    roomId = roomId,
                    worldReadable = false,
                    joinRule = joinRule
                )
            )
        )

    private fun TestScope.searchGroupViewModel() =
        SearchGroupViewModelFactory.create(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            matrixClientMock, UserId("test", "server")
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = backgroundScope.coroutineContext,
            ),
            onGroupJoined = onGroupJoinedMock,
            onGroupKnocked = onGroupKnockedMock,
            onBack = mock()
        )

    private suspend fun loadData(cut: SearchGroupViewModel) {
        cut.foundGroups.firstOrNull { it.isNotEmpty() }
    }
}
