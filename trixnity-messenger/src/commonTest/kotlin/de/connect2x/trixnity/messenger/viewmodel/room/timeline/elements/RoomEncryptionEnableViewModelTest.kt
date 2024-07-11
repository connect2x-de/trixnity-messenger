package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import org.koin.dsl.koinApplication
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomEncryptionEnableViewModelTest : ShouldSpec() {

    val matrixClientMock = mock<MatrixClient>()

    init {
        coroutineTestScope = true
        beforeTest {
            resetMocks(matrixClientMock)
        }

        should("display who enabled to end-to-end encryption") {
            val cut = roomEncryptionEnableViewModel(coroutineContext = coroutineContext)
            val subscriberJob = launch { cut.roomEncryptionEnableMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()
            cut.roomEncryptionEnableMessage.value shouldBe "Bob enabled end-to-end encryption"
            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to username changes") {
            val userFlow = MutableStateFlow(UserInfoElement("Bob", UserId("bob:localhost")))
            val cut = roomEncryptionEnableViewModel(userFlow = userFlow, coroutineContext = coroutineContext)
            val subscriberJob = launch { cut.roomEncryptionEnableMessage.collect {} }
            userFlow.value = UserInfoElement("Bobby", UserId("booby:localhost"))
            testCoroutineScheduler.advanceUntilIdle()
            cut.roomEncryptionEnableMessage.value shouldBe "Bobby enabled end-to-end encryption"
            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private fun roomEncryptionEnableViewModel(
        timelineEvent: TimelineEvent = timelineEvent(),
        userFlow: StateFlow<UserInfoElement> = MutableStateFlow(UserInfoElement("Bob", UserId("bob:localhost"))),
        isDirectFlow: StateFlow<Boolean> = MutableStateFlow(false),
        coroutineContext: CoroutineContext
    ): RoomEncryptionEnableViewModel =
        RoomEncryptionEnableViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
                    )
                }.koin,
                userId = UserId("user1", "server"),
                coroutineContext = coroutineContext
            ),
            timelineEvent = timelineEvent,
            content = timelineEvent.event.content as EncryptionEventContent,
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(""),
            sender = userFlow,
            isDirectFlow = isDirectFlow
        )

    private fun timelineEvent() = TimelineEvent(
        event = ClientEvent.RoomEvent.StateEvent(
            EncryptionEventContent(),
            id = EventId(""),
            sender = UserId(""),
            roomId = RoomId(""),
            originTimestamp = 0L,
            unsigned = null,
            stateKey = ""
        ),
        content = null,
        previousEventId = null,
        nextEventId = null,
        gap = null
    )

}
