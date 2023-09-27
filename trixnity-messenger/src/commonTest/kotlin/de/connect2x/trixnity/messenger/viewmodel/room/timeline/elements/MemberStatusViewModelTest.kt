package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MemberStatusViewModelTest : ShouldSpec() {

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var userServiceMock: UserService

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
                            single { userServiceMock }
                        }
                    )
                }.koin
                every {
                    userServiceMock.getById(isAny(), isEqual(UserId("bob", "localhost")))
                } returns MutableStateFlow(
                    RoomUser(
                        roomId = RoomId("room1", "localhost"),
                        userId = UserId("bob", "localhost"),
                        name = "Bob",
                        event = Event.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId(""),
                            sender = UserId(""),
                            roomId = RoomId(""),
                            originTimestamp = 0L,
                            stateKey = "",
                        ),
                    )
                )
                every {
                    userServiceMock.getById(isAny(), isEqual(UserId("mallory", "localhost")))
                } returns MutableStateFlow(
                    RoomUser(
                        roomId = RoomId("room1", "localhost"),
                        userId = UserId("mallory", "localhost"),
                        name = "Mallory",
                        event = Event.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId(""),
                            sender = UserId(""),
                            roomId = RoomId(""),
                            originTimestamp = 0L,
                            stateKey = "",
                        ),
                    )
                )
            }
        }

        should("not show an indicator for name changes") {
            val cut = memberStatusViewModel(
                timelineEventFlow = MutableStateFlow(
                    memberStateTimelineEvent(
                        displayName = "I have changed!",
                        previousMemberEventContent = memberEventContent(displayName = "I am the original"),
                        stateKey = "@bob:localhost",
                    )
                ),
                coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.formattedMemberStatus.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.formattedMemberStatus.value shouldBe null

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for avatar image changes") {
            val cut = memberStatusViewModel(
                timelineEventFlow = MutableStateFlow(
                    memberStateTimelineEvent(
                        avatarUrl = "mxc://localhost/new_url",
                        previousMemberEventContent = memberEventContent(avatarUrl = "mxc://localhost/boring_old_url"),
                        stateKey = "@bob:localhost",
                    )
                ),
                usernameFlow = MutableStateFlow("Bob"),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.formattedMemberStatus.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.formattedMemberStatus.value shouldBe "Bob has changed the avatar image"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for user joining a room") {
            val cut = memberStatusViewModel(
                timelineEventFlow = MutableStateFlow(
                    memberStateTimelineEvent(
                        membership = Membership.JOIN,
                        previousMemberEventContent = null,
                        stateKey = "@bob:localhost",
                    )
                ),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.formattedMemberStatus.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.formattedMemberStatus.value shouldBe "Bob has joined the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for user leaving a room") {
            val cut = memberStatusViewModel(
                timelineEventFlow = MutableStateFlow(
                    memberStateTimelineEvent(
                        membership = Membership.LEAVE,
                        previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                        stateKey = "@bob:localhost",
                    )
                ),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.formattedMemberStatus.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.formattedMemberStatus.value shouldBe "Bob has left the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for user being banned from a room") {
            val cut = memberStatusViewModel(
                timelineEventFlow = MutableStateFlow(
                    memberStateTimelineEvent(
                        membership = Membership.BAN,
                        previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                        stateKey = "@mallory:localhost",
                    )
                ),
                usernameFlow = MutableStateFlow("User1"),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.formattedMemberStatus.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.formattedMemberStatus.value shouldBe "Mallory has been removed by User1 from the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for an invitation of a user to the room") {
            val cut = memberStatusViewModel(
                timelineEventFlow = MutableStateFlow(
                    memberStateTimelineEvent(
                        membership = Membership.INVITE,
                        previousMemberEventContent = null,
                        stateKey = "@bob:localhost",
                    )
                ),
                usernameFlow = MutableStateFlow("User1"),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.formattedMemberStatus.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.formattedMemberStatus.value shouldBe "Bob has been invited by User1"
            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for a user knocking at the room") {
            val cut = memberStatusViewModel(
                timelineEventFlow = MutableStateFlow(
                    memberStateTimelineEvent(
                        membership = Membership.KNOCK,
                        previousMemberEventContent = null,
                        stateKey = "@bob:localhost",
                    )
                ),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.formattedMemberStatus.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.formattedMemberStatus.value shouldBe "Bob wants to join the group"
            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("update indicator on username changes") {
            val usernameFlow = MutableStateFlow("User1")
            val cut = memberStatusViewModel(
                timelineEventFlow = MutableStateFlow(
                    memberStateTimelineEvent(
                        membership = Membership.INVITE,
                        previousMemberEventContent = null,
                        stateKey = "@bob:localhost",
                    )
                ),
                usernameFlow = usernameFlow,
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.formattedMemberStatus.collect {} }
            usernameFlow.value = "User1 new name"
            testCoroutineScheduler.advanceUntilIdle()

            cut.formattedMemberStatus.value shouldBe "Bob has been invited by User1 new name"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("update indicator on room changing 'direct' state") {
            val isDirectFlow = MutableStateFlow(false)
            val cut = memberStatusViewModel(
                timelineEventFlow = MutableStateFlow(
                    memberStateTimelineEvent(
                        membership = Membership.JOIN,
                        previousMemberEventContent = null,
                        stateKey = "@bob:localhost",
                    )
                ),
                isDirectFlow = isDirectFlow,
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.formattedMemberStatus.collect {} }
            isDirectFlow.value = true
            testCoroutineScheduler.advanceUntilIdle()

            cut.formattedMemberStatus.value shouldBe "Bob has joined the chat"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("update indicator when the underlying timeline event changes") {
            val timelineEventFlow = MutableStateFlow(
                memberStateTimelineEvent(
                    membership = Membership.JOIN,
                    previousMemberEventContent = null,
                    stateKey = "@bob:localhost",
                )
            )
            val cut = memberStatusViewModel(timelineEventFlow = timelineEventFlow, coroutineContext = coroutineContext)
            val subscriberJob = launch { cut.formattedMemberStatus.collect {} }
            timelineEventFlow.value = memberStateTimelineEvent(
                membership = Membership.KNOCK,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
            )
            testCoroutineScheduler.advanceUntilIdle()

            cut.formattedMemberStatus.value shouldBe "Bob wants to join the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private fun memberStatusViewModel(
        timelineEventFlow: StateFlow<TimelineEvent?>,
        usernameFlow: StateFlow<String> = MutableStateFlow(""),
        isDirectFlow: StateFlow<Boolean> = MutableStateFlow(false),
        coroutineContext: CoroutineContext,
    ): MemberStatusViewModelImpl {

        val di = koinApplication {
            modules(
                trixnityMessengerModule(),
                testMatrixClientModule(matrixClientMock),
            )
        }.koin
        di.get<I18n>().setCurrentLang("en")
        return MemberStatusViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                accountName = "test",
                coroutineContext = coroutineContext
            ),
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(""),
            timelineEventFlow = timelineEventFlow,
            usernameFlow = usernameFlow,
            isDirectFlow = isDirectFlow,
        )
    }

    private fun memberStateTimelineEvent(
        avatarUrl: String = "",
        displayName: String = "Bob",
        membership: Membership = Membership.JOIN,
        isDirect: Boolean = false,
        stateKey: String = "",
        previousMemberEventContent: MemberEventContent? = null,
    ) =
        TimelineEvent(
            event = Event.StateEvent(
                content = MemberEventContent(
                    avatarUrl = avatarUrl,
                    displayName = displayName,
                    membership = membership,
                    isDirect = isDirect,
                ),
                id = EventId(""),
                sender = UserId(""),
                roomId = RoomId(""),
                originTimestamp = 0L,
                unsigned = UnsignedRoomEventData.UnsignedStateEventData(
                    previousContent = previousMemberEventContent,
                ),
                stateKey = stateKey,
            ),
            content = null,
            roomId = RoomId(""),
            eventId = EventId(""),
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )

    private fun memberEventContent(
        avatarUrl: String = "",
        displayName: String = "Bob",
        membership: Membership = Membership.JOIN,
        isDirect: Boolean = false,
    ) = MemberEventContent(
        avatarUrl = avatarUrl,
        displayName = displayName,
        membership = membership,
        isDirect = isDirect,
    )
}