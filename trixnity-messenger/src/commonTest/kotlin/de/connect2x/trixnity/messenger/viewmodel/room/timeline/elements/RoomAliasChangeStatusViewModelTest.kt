package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.CanonicalAliasStateTimelineElementViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData.UnsignedStateEventData
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import org.koin.dsl.koinApplication
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomAliasChangeStatusViewModelTest : ShouldSpec() {

    val matrixClientMock = mock<MatrixClient>()
    val alias1 = RoomAliasId("alias1", "localhost")
    val alias2 = RoomAliasId("alias2", "localhost")
    val alias3 = RoomAliasId("alias3", "localhost")
    val alias4 = RoomAliasId("alias4", "localhost")
    val alias5 = RoomAliasId("alias5", "localhost")

    val user = UserInfoElement("user", UserId("user", "localhost"))

    lateinit var i18n: I18n

    init {
        beforeTest {
            i18n = object : I18n(
                DefaultLanguages,
                createTestMatrixMessengerSettingsHolder(),
                GetSystemLang { "en" },
                TimeZone.of("CET"),
            ) {}
            resetMocks(matrixClientMock)
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
                cut.changeMessage.value shouldBe listOf(i18n.setAsMainAlias(user.name, alias1.full))
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
                cut.changeMessage.value shouldBe listOf(i18n.setAsMainAlias(user.name, alias1.full))
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
                cut.changeMessage.value shouldBe listOf(i18n.setAsMainAlias(user.name, alias1.full))
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
                cut.changeMessage.value shouldBe listOf(i18n.removeAsMainAlias(user.name, alias2.full))
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
                cut.changeMessage.value shouldBe listOf(i18n.removedAlias(user.name, alias2.full))
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
                cut.changeMessage.value shouldBe listOf(i18n.addedAlias(user.name, alias4.full))
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
                    i18n.addedAlias(user.name, alias4.full),
                    i18n.addedAlias(user.name, alias5.full)
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
                cut.changeMessage.value shouldBe listOf(i18n.removedAlias(user.name, alias4.full))
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
                    i18n.removedAlias(user.name, alias4.full),
                    i18n.removedAlias(user.name, alias5.full)
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
        isDirectFlow: StateFlow<Boolean> = MutableStateFlow(false),
        coroutineContext: CoroutineContext,
    ): CanonicalAliasStateTimelineElementViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        return CanonicalAliasStateTimelineElementViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            timelineEvent = TimelineEvent(
                event = StateEvent(
                    eventContent,
                    id = EventId(""),
                    sender = UserId(""),
                    roomId = RoomId(""),
                    originTimestamp = 0L,
                    unsigned = UnsignedStateEventData(previousContent = previousEventContent),
                    stateKey = ""
                ),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            ),
            content = eventContent,
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(""),
            sender = flowOf(user),
            isDirectFlow = isDirectFlow,
        )
    }
}
