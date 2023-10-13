package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.trixnityMessengerModule
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
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomCreatedStatusViewModelTest : ShouldSpec() {

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    init {
        Dispatchers.setMain(testMainDispatcher)
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)
        }

        should("show indicator for room creation") {
            val cut = roomCreatedStatusViewModel(
                usernameFlow = MutableStateFlow("Bob"),
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
            val usernameFlow = MutableStateFlow("Bob")
            val cut = roomCreatedStatusViewModel(
                usernameFlow = usernameFlow,
                isDirectFlow = MutableStateFlow(false),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.roomCreatedMessage.collect {} }
            usernameFlow.value = "Bobby"
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomCreatedMessage.value shouldBe "Bobby has created the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to room's direct changes") {
            val isDirectFlow = MutableStateFlow(false)
            val cut = roomCreatedStatusViewModel(
                usernameFlow = MutableStateFlow("Bob"),
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

    private fun roomCreatedStatusViewModel(
        usernameFlow: StateFlow<String>,
        isDirectFlow: StateFlow<Boolean>,
        coroutineContext: CoroutineContext
    ): RoomCreatedStatusViewModelImpl {

        val di = koinApplication {
            modules(
                trixnityMessengerModule(),
                testMatrixClientModule(matrixClientMock),
            )
        }.koin
        di.get<I18n>().setCurrentLang("en")
        return RoomCreatedStatusViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                accountName = "test",
                coroutineContext = coroutineContext
            ),
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(""),
            sender = usernameFlow,
            isDirectFlow = isDirectFlow,
        )
    }
}