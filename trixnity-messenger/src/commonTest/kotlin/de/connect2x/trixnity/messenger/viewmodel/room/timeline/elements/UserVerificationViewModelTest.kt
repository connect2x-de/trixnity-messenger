package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerifications
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModelFactory
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.verification.ActiveVerification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code.Timeout
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedEventContent.MegolmEncryptedEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.VerificationRequestMessageEventContent
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.KeyAlgorithm
import org.kodein.mock.*
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("UNUSED_CHANGED_VALUE")
class UserVerificationViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    private val thisRoom = RoomId("room", "localhost")
    private val timelineEventId = EventId("event-0")
    private val me = UserId("me", "localhost")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var activeVerifications: ActiveVerifications

    @Mock
    lateinit var verificationViewModel: VerificationViewModel

    @Mock
    lateinit var activeVerification: ActiveVerification

    @Fake
    lateinit var verificationRequestMessageEventContent: VerificationRequestMessageEventContent

    @Fake
    lateinit var ready: ActiveVerificationState.Ready

    init {
        Dispatchers.setMain(testMainDispatcher)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.userId } returns me
            }
        }

        should("show 'from us' when verification request targeted at other user") {
            mocker.everySuspending {
                activeVerifications.getActiveVerification(isAny(), isEqual(thisRoom), isEqual(timelineEventId))
            } returns activeVerification
            val cut = userVerificationViewModel(
                verificationRequestMessageEventContent.copy(
                    to = UserId("other", "localhost")
                )
            )

            cut.sender.value shouldBe "us"
        }

        should("show other user's name when the request is targeted at us") {
            val cut = userVerificationViewModel(verificationRequestMessageEventContent.copy(to = me))

            cut.sender.value shouldBe "username"
        }

        should("show as active when the verification has not timed out and is not done or cancelled") {
            mocker.every { activeVerification.state } returns MutableStateFlow(ready)
            mocker.everySuspending {
                activeVerifications.getActiveVerification(isAny(), isEqual(thisRoom), isEqual(timelineEventId))
            } returns activeVerification
            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.isActive.first { it }
        }

        should("show as inactive when verification has timed out") {
            mocker.everySuspending {
                activeVerifications.getActiveVerification(
                    isEqual(matrixClientMock),
                    isEqual(thisRoom),
                    isEqual(timelineEventId)
                )
            } returns null
            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.isActive.first { it.not() }
        }

        should("show as inactive when the verification has not timed out, but is done or cancelled") {
            mocker.every { activeVerification.state } returns MutableStateFlow(ActiveVerificationState.Done)
            mocker.everySuspending {
                activeVerifications.getActiveVerification(
                    isEqual(matrixClientMock),
                    isEqual(thisRoom),
                    isEqual(timelineEventId)
                )
            } returns activeVerification
            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.isActive.first { it.not() }
        }

        should("search for correct end event for an inactive verification") {
            mocker.everySuspending {
                activeVerifications.getActiveVerification(
                    isEqual(matrixClientMock),
                    isEqual(thisRoom),
                    isEqual(timelineEventId)
                )
            } returns null
            mocker.every {
                roomServiceMock.getTimelineEvent(
                    isEqual(thisRoom), isEqual(timelineEventId), isAny(),
                )
            } returns MutableStateFlow(
                timelineEvent(timelineEventId)
            )
            mocker.every { roomServiceMock.getTimelineEvents(isAny(), isAny(), isAny(), isAny()) } returns
                    flow {
                        emit(
                            flowOf(
                                TimelineEvent(
                                    event = MessageEvent(
                                        content = VerificationCancelEventContent(
                                            code = Timeout,
                                            reason = "",
                                            relatesTo = RelatesTo.Reference(timelineEventId),
                                            transactionId = null
                                        ),
                                        EventId("cancel"),
                                        me,
                                        thisRoom,
                                        0L,
                                    ),
                                    content = null,
                                    roomId = thisRoom,
                                    eventId = EventId("cancel"),
                                    previousEventId = null,
                                    nextEventId = null,
                                    gap = null,
                                )
                            )
                        )
                    }

            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.reachedEndState.filterNotNull().first { it.first.not() }
            cut.reachedEndState.filterNotNull().first { it.second == "Timeout" }
        }

        should("interpret the end state as 'cancelled' when the corresponding end event for an inactive verification cannot be found in the next 40 messages") {
            mocker.everySuspending {
                activeVerifications.getActiveVerification(
                    isEqual(matrixClientMock),
                    isEqual(thisRoom),
                    isEqual(timelineEventId)
                )
            } returns null

            var eventIdNo = 0
            mocker.every {
                roomServiceMock.getTimelineEvent(
                    isEqual(thisRoom),
                    isEqual(timelineEventId),
                    isAny(),
                )
            } returns MutableStateFlow(timelineEvent(timelineEventId))

            mocker.every { roomServiceMock.getTimelineEvents(isAny(), isAny(), isAny(), isAny()) } returns
                    flow {
                        (0..40).forEach { eventIdNo ->
                            emit(
                                flowOf(
                                    timelineEventMessage(
                                        EventId("event-${eventIdNo}"),
                                        TextMessageEventContent("")
                                    )
                                )
                            )
                        }
                    }

            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.reachedEndState.first { it?.first == false }
            cut.reachedEndState.first { it?.second == "Cancelled" }
        }

        should("consider encrypted timeline events in the search for end events of inactive verification")
        {
            mocker.everySuspending {
                activeVerifications.getActiveVerification(
                    isEqual(matrixClientMock),
                    isEqual(thisRoom),
                    isEqual(timelineEventId)
                )
            } returns null

            mocker.every {
                roomServiceMock.getTimelineEvent(
                    isEqual(thisRoom),
                    isEqual(timelineEventId),
                    isAny(),
                )
            } returns MutableStateFlow(timelineEvent(timelineEventId))

            mocker.every { roomServiceMock.getTimelineEvents(isAny(), isAny(), isAny(), isAny()) } returns
                    flow {
                        emit(
                            flowOf(
                                TimelineEvent(
                                    event = MessageEvent(
                                        content = MegolmEncryptedEventContent(
                                            ciphertext = "",
                                            senderKey = Key.Curve25519Key(
                                                value = "",
                                                algorithm = KeyAlgorithm.Curve25519
                                            ),
                                            deviceId = "",
                                            sessionId = "",
                                        ),
                                        id = EventId(""),
                                        sender = me,
                                        roomId = thisRoom,
                                        originTimestamp = 0L,
                                    ),
                                    content = Result.success(
                                        VerificationCancelEventContent(
                                            code = Timeout,
                                            reason = "",
                                            relatesTo = RelatesTo.Reference(timelineEventId),
                                            transactionId = "",
                                        )
                                    ),
                                    roomId = thisRoom,
                                    eventId = EventId(""),
                                    previousEventId = null,
                                    nextEventId = null,
                                    gap = null,
                                )
                            )
                        )
                    }

            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.reachedEndState.filterNotNull().first { it.first.not() }
        }

        should("ignore end events of other verifications")
        {
            mocker.everySuspending {
                activeVerifications.getActiveVerification(
                    isEqual(matrixClientMock),
                    isEqual(thisRoom),
                    isEqual(timelineEventId)
                )
            } returns null

            mocker.every {
                roomServiceMock.getTimelineEvent(
                    isEqual(thisRoom),
                    isEqual(timelineEventId),
                    isAny(),
                )
            } returns MutableStateFlow(timelineEvent(timelineEventId))
            val otherEventId = EventId("completely different")

            mocker.every { roomServiceMock.getTimelineEvents(isAny(), isAny(), isAny(), isAny()) } returns
                    flow {
                        emit(
                            flowOf(
                                timelineEventMessage(
                                    otherEventId,
                                    VerificationDoneEventContent(
                                        relatesTo = RelatesTo.Reference(otherEventId),
                                        transactionId = null
                                    )
                                )
                            )
                        )
                        emit(
                            flowOf(
                                timelineEventMessage(
                                    otherEventId,
                                    VerificationCancelEventContent(
                                        code = Timeout,
                                        reason = "",
                                        relatesTo = RelatesTo.Reference(timelineEventId),
                                        transactionId = null
                                    ),
                                )
                            )
                        )
                    }

            val cut = userVerificationViewModel(verificationRequestMessageEventContent)

            cut.reachedEndState.filterNotNull().first { it.first.not() }
        }
    }

    private fun userVerificationViewModel(
        verificationRequestMessageEventContent: VerificationRequestMessageEventContent
    ): UserVerificationViewModelImpl {
        val di = koinApplication {
            modules(trixnityMessengerModule(), testMatrixClientModule(matrixClientMock, me.full), module {
                single { activeVerifications }
                single<VerificationViewModelFactory> {
                    object : VerificationViewModelFactory {
                        override fun newVerificationViewModel(
                            viewModelContext: MatrixClientViewModelContext,
                            onCloseVerification: () -> Unit,
                            onRedoSelfVerification: () -> Unit,
                            roomId: RoomId?,
                            timelineEventId: EventId?,
                        ): VerificationViewModel = verificationViewModel
                    }
                }
            })
        }.koin
        di.get<I18n>().setCurrentLang("en")
        return UserVerificationViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                accountName = "@me:localhost",
                coroutineContext = Dispatchers.Unconfined
            ),
            invitation = MutableStateFlow(""),
            formattedDate = "",
            showDateAbove = false,
            formattedTime = null,
            usernameFlow = MutableStateFlow("username"),
            content = verificationRequestMessageEventContent,
            selectedRoomId = thisRoom,
            timelineEventId = timelineEventId,
        )
    }

    private fun timelineEvent(eventId: EventId) = TimelineEvent(
        event = StateEvent(
            content = MemberEventContent(membership = Membership.JOIN),
            id = eventId,
            sender = me,
            roomId = thisRoom,
            originTimestamp = 0L,
            stateKey = ""
        ),
        content = null,
        roomId = thisRoom,
        eventId = eventId,
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )

    private fun timelineEventMessage(
        eventId: EventId,
        messageEventContent: MessageEventContent,
    ) = TimelineEvent(
        event = MessageEvent(
            content = messageEventContent,
            id = eventId,
            sender = me,
            roomId = thisRoom,
            originTimestamp = 0L,
        ),
        content = null,
        roomId = thisRoom,
        eventId = eventId,
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )

    private fun ArgConstraintsBuilder.isTimelineEventUntil50(
        capture: MutableList<TimelineEvent>? = null
    ): TimelineEvent =
        isValid(ArgConstraint(capture, { "isTimelineEventUntil50" }) {
            if (it.eventId.full.split("-")[1].toInt() < 50) ArgConstraint.Result.Success
            else ArgConstraint.Result.Failure { "Expected timelineEvent with Id < 50, but got $it" }
        })

    private fun ArgConstraintsBuilder.isTimelineEvent50(
        capture: MutableList<TimelineEvent>? = null
    ): TimelineEvent =
        isValid(ArgConstraint(capture, { "isTimelineEvent50" }) {
            if (it.eventId.full.split("-")[1].toInt() == 50) ArgConstraint.Result.Success
            else ArgConstraint.Result.Failure { "Expected timelineEvent with Id == 50, but got $it" }
        })


}