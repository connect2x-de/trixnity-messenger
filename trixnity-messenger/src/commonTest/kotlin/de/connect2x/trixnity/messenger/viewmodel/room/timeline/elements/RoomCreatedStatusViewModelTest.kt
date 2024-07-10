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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import org.koin.dsl.koinApplication
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomCreatedStatusViewModelTest : ShouldSpec() {

    val matrixClientMock = mock<MatrixClient>()

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(matrixClientMock)
        }

        should("show indicator for room creation") {
            val cut = roomCreatedStatusViewModel(
                usernameFlow = MutableStateFlow(UserInfoElement("Bob", UserId("bob:localhost"))),
                isDirectFlow = MutableStateFlow(false),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.roomCreatedMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomCreatedMessage.value shouldBe "Bob has created the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to username changes`") {
            val usernameFlow = MutableStateFlow(UserInfoElement("Bob", UserId("bob:localhost")))
            val cut = roomCreatedStatusViewModel(
                usernameFlow = usernameFlow,
                isDirectFlow = MutableStateFlow(false),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.roomCreatedMessage.collect {} }
            usernameFlow.value = UserInfoElement("Bobby", UserId("bobby:localhost"))
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomCreatedMessage.value shouldBe "Bobby has created the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to room's direct changes") {
            val isDirectFlow = MutableStateFlow(false)
            val cut = roomCreatedStatusViewModel(
                usernameFlow = MutableStateFlow(UserInfoElement("Bob", UserId("bob:localhost"))),
                isDirectFlow = isDirectFlow,
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.roomCreatedMessage.collect {} }
            isDirectFlow.value = true
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomCreatedMessage.value shouldBe "Bob has created the chat"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun roomCreatedStatusViewModel(
        usernameFlow: StateFlow<UserInfoElement>,
        isDirectFlow: StateFlow<Boolean>,
        coroutineContext: CoroutineContext
    ): RoomCreatedStatusViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        return RoomCreatedStatusViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            timelineEvent = null,
            content = CreateEventContent(creator = UserId("me", "local")),
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(""),
            sender = usernameFlow,
            isDirectFlow = isDirectFlow,
        )
    }
}
