package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
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
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class CanonicalAliasStateTimelineElementViewModelTest : ShouldSpec() {

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

    lateinit var i18n: I18n

    init {
        beforeTest {
            i18n = object : I18n(
                DefaultLanguages,
                createTestMatrixMessengerSettingsHolder(),
                GetSystemLang { "en" },
                TimeZone.Companion.of("CET"),
            ) {}
            resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                    }
                )
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

        should("display main alias switch") {
            val cut = roomAliasChangeStatusViewModel(
                previousEventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3)
                ),
                eventContent = CanonicalAliasEventContent(
                    alias = alias1,
                    aliases = setOf(alias2, alias3)
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }

            eventually(2.seconds) {
                cut.changeMessage.value shouldBe listOf(i18n.setAsMainAlias("bob", alias1.full))
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display main alias creation based on alt_alias") {
            val cut = roomAliasChangeStatusViewModel(
                previousEventContent = CanonicalAliasEventContent(
                    alias = null,
                    aliases = setOf(alias1, alias2)
                ),
                eventContent = CanonicalAliasEventContent(
                    alias = alias1,
                    aliases = setOf(alias2)
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }

            eventually(2.seconds) {
                cut.changeMessage.value shouldBe listOf(i18n.setAsMainAlias("bob", alias1.full))
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display main alias creation out of nowhere") {
            val cut = roomAliasChangeStatusViewModel(
                previousEventContent = CanonicalAliasEventContent(
                    alias = null,
                    aliases = setOf(alias2)
                ),
                eventContent = CanonicalAliasEventContent(
                    alias = alias1,
                    aliases = setOf(alias2)
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }

            eventually(2.seconds) {
                cut.changeMessage.value shouldBe listOf(i18n.setAsMainAlias("bob", alias1.full))
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display main alias removal") {
            val cut = roomAliasChangeStatusViewModel(
                previousEventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3)
                ),
                eventContent = CanonicalAliasEventContent(
                    alias = null,
                    aliases = setOf(alias2, alias1, alias3)
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }

            eventually(2.seconds) {
                cut.changeMessage.value shouldBe listOf(i18n.removeAsMainAlias("bob", alias2.full))
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display main alias deletion") {
            val cut = roomAliasChangeStatusViewModel(
                previousEventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3)
                ),
                eventContent = CanonicalAliasEventContent(
                    alias = null,
                    aliases = setOf(alias1, alias3)
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }

            eventually(2.seconds) {
                cut.changeMessage.value shouldBe listOf(i18n.removedAlias("bob", alias2.full))
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display added alias") {
            val cut = roomAliasChangeStatusViewModel(
                previousEventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3)
                ),
                eventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3, alias4)
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }

            eventually(2.seconds) {
                cut.changeMessage.value shouldBe listOf(i18n.addedAlias("bob", alias4.full))
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display added aliases") {
            val cut = roomAliasChangeStatusViewModel(
                previousEventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3)
                ),
                eventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3, alias4, alias5)
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }

            eventually(2.seconds) {
                cut.changeMessage.value shouldBe listOf(
                    i18n.addedAlias("bob", alias4.full),
                    i18n.addedAlias("bob", alias5.full)
                )
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }


        should("display removed alias") {
            val cut = roomAliasChangeStatusViewModel(
                previousEventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3, alias4)
                ),
                eventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3)
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }

            eventually(2.seconds) {
                cut.changeMessage.value shouldBe listOf(i18n.removedAlias("bob", alias4.full))
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display removed aliases") {
            val cut = roomAliasChangeStatusViewModel(
                previousEventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3, alias4, alias5)
                ),
                eventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3)
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }

            eventually(2.seconds) {
                cut.changeMessage.value shouldBe listOf(
                    i18n.removedAlias("bob", alias4.full),
                    i18n.removedAlias("bob", alias5.full)
                )
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("do nothing when nothing changed") {
            val cut = roomAliasChangeStatusViewModel(
                previousEventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3)
                ),
                eventContent = CanonicalAliasEventContent(
                    alias = alias2,
                    aliases = setOf(alias1, alias3)
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }

            eventually(2.seconds) {
                cut.changeMessage.value shouldBe listOf()
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun roomAliasChangeStatusViewModel(
        eventContent: CanonicalAliasEventContent,
        previousEventContent: CanonicalAliasEventContent,
        coroutineContext: CoroutineContext,
    ): CanonicalAliasStateTimelineElementViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher.Key]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
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
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            content = eventContent,
            roomId,
            eventId,
        )
    }
}
