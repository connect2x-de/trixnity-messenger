package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class CanonicalAliasStateTimelineElementViewModelTest {

    val alias1 = RoomAliasId("alias1", "localhost")
    val alias2 = RoomAliasId("alias2", "localhost")
    val alias3 = RoomAliasId("alias3", "localhost")
    val alias4 = RoomAliasId("alias4", "localhost")
    val alias5 = RoomAliasId("alias5", "localhost")

    val roomId = RoomId("room", "server")
    val userId = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    var i18n = object : I18n(
        DefaultLanguages,
        createTestMatrixMessengerSettingsHolder(),
        GetSystemLang { "en" },
        TimeZone.Companion.of("CET"),
    ) {}

    init {
        resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        every { userServiceMock.getById(roomId, userId) } returns flowOf(
            RoomUser(
                roomId, userId, "bob", StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = eventId,
                    sender = userId,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        )
        every { roomServiceMock.getById(roomId) } returns flowOf(Room(roomId, isDirect = true))
    }

    @Test
    fun `display main alias switch`() = runTest {
        val cut = roomAliasChangeStatusViewModel(
            previousEventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3)
            ),
            eventContent = CanonicalAliasEventContent(
                alias = alias1, aliases = setOf(alias2, alias3)
            ),
        )

        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(2.seconds) {
            cut.changeMessage.value shouldBe listOf(i18n.setAsMainAlias("bob", alias1.full))
        }
    }

    @Test
    fun `display main alias creation based on alt_alias`() = runTest {
        val cut = roomAliasChangeStatusViewModel(
            previousEventContent = CanonicalAliasEventContent(
                alias = null, aliases = setOf(alias1, alias2)
            ),
            eventContent = CanonicalAliasEventContent(
                alias = alias1, aliases = setOf(alias2)
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(2.seconds) {
            cut.changeMessage.value shouldBe listOf(i18n.setAsMainAlias("bob", alias1.full))
        }
    }

    @Test
    fun `display main alias creation out of nowhere`() = runTest {
        val cut = roomAliasChangeStatusViewModel(
            previousEventContent = CanonicalAliasEventContent(
                alias = null, aliases = setOf(alias2)
            ),
            eventContent = CanonicalAliasEventContent(
                alias = alias1, aliases = setOf(alias2)
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(2.seconds) {
            cut.changeMessage.value shouldBe listOf(i18n.setAsMainAlias("bob", alias1.full))
        }
    }

    @Test
    fun `display main alias removal`() = runTest {
        val cut = roomAliasChangeStatusViewModel(
            previousEventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3)
            ),
            eventContent = CanonicalAliasEventContent(
                alias = null, aliases = setOf(alias2, alias1, alias3)
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(2.seconds) {
            cut.changeMessage.value shouldBe listOf(i18n.removeAsMainAlias("bob", alias2.full))
        }
    }

    @Test
    fun `display main alias deletion`() = runTest {
        val cut = roomAliasChangeStatusViewModel(
            previousEventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3)
            ),
            eventContent = CanonicalAliasEventContent(
                alias = null, aliases = setOf(alias1, alias3)
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(2.seconds) {
            cut.changeMessage.value shouldBe listOf(i18n.removedAlias("bob", alias2.full))
        }
    }

    @Test
    fun `display added alias`() = runTest {
        val cut = roomAliasChangeStatusViewModel(
            previousEventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3)
            ),
            eventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3, alias4)
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(2.seconds) {
            cut.changeMessage.value shouldBe listOf(i18n.addedAlias("bob", alias4.full))
        }
    }

    @Test
    fun `display added aliases`() = runTest {
        val cut = roomAliasChangeStatusViewModel(
            previousEventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3)
            ),
            eventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3, alias4, alias5)
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(2.seconds) {
            cut.changeMessage.value shouldBe listOf(
                i18n.addedAlias("bob", alias4.full), i18n.addedAlias("bob", alias5.full)
            )
        }
    }


    @Test
    fun `display removed alias`() = runTest {
        val cut = roomAliasChangeStatusViewModel(
            previousEventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3, alias4)
            ),
            eventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3)
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(2.seconds) {
            cut.changeMessage.value shouldBe listOf(i18n.removedAlias("bob", alias4.full))
        }
    }

    @Test
    fun `display removed aliases`() = runTest {
        val cut = roomAliasChangeStatusViewModel(
            previousEventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3, alias4, alias5)
            ),
            eventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3)
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(2.seconds) {
            cut.changeMessage.value shouldBe listOf(
                i18n.removedAlias("bob", alias4.full), i18n.removedAlias("bob", alias5.full)
            )
        }
    }

    @Test
    fun `do nothing when nothing changed`() = runTest {
        val cut = roomAliasChangeStatusViewModel(
            previousEventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3)
            ),
            eventContent = CanonicalAliasEventContent(
                alias = alias2, aliases = setOf(alias1, alias3)
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(2.seconds) {
            cut.changeMessage.value shouldBe listOf()
        }
    }

    private fun TestScope.roomAliasChangeStatusViewModel(
        eventContent: CanonicalAliasEventContent,
        previousEventContent: CanonicalAliasEventContent,
    ): CanonicalAliasStateTimelineElementViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(UserId("test", "server") to matrixClientMock)
                )
            )
        }.koin
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(
            TimelineEvent(
                event = StateEvent(
                    eventContent,
                    id = eventId,
                    sender = userId,
                    roomId = roomId,
                    originTimestamp = 0L,
                    unsigned = UnsignedRoomEventData.UnsignedStateEventData(previousContent = previousEventContent),
                    stateKey = ""
                ),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        )
        return CanonicalAliasStateTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = UserId("test", "server"),
            ),
            content = eventContent,
            roomId,
            eventId,
        )
    }
}
