package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import io.kotest.assertions.nondeterministic.continually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.KeyAlgorithm
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class EncryptedMessageViewModelTest : ShouldSpec() {
    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    init {
        Dispatchers.setMain(testMainDispatcher)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)
        }

        should("show wait for decryption if the encrypted element is not yet decrypted") {
            val cut = encryptedMessageViewModel(MutableStateFlow(timelineEvent(null)))
            val subscriberJob = launch { cut.waitForDecryption.collect {} }

            continually(200.milliseconds) {
                cut.waitForDecryption.value shouldBe true
            }
            subscriberJob.cancel()
        }

        should("not wait for decryption if the decryption failed") {
            val cut =
                encryptedMessageViewModel(MutableStateFlow(timelineEvent(Result.failure(RuntimeException("Oh no!")))))

            cut.waitForDecryption.first { it.not() }
        }

        should("not wait for decryption when the encrypted event could be decrypted successfully") {
            val cut =
                encryptedMessageViewModel(
                    MutableStateFlow(
                        timelineEvent(
                            Result.success(
                                RoomMessageEventContent.TextBased.Text(
                                    ""
                                )
                            )
                        )
                    )
                )

            cut.waitForDecryption.first { it.not() }
        }

        should("react to changes of the timeline event") {
            val timelineEventFlow = MutableStateFlow(timelineEvent(null))
            val cut = encryptedMessageViewModel(timelineEventFlow)
            val subscriberJob = launch { cut.waitForDecryption.collect {} }

            continually(200.milliseconds) {
                cut.waitForDecryption.value shouldBe true
            }
            timelineEventFlow.value = timelineEvent(Result.success(RoomMessageEventContent.TextBased.Text("")))
            cut.waitForDecryption.first { it.not() }

            subscriberJob.cancel()
        }
    }

    private fun encryptedMessageViewModel(timelineEventFlow: StateFlow<TimelineEvent?>) =
        EncryptedMessageViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)),
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = Dispatchers.Unconfined,
            ),
            formattedDate = "",
            showDateAbove = false,
            formattedTime = null,
            isByMe = false,
            showChatBubbleEdge = false,
            showBigGap = false,
            showSender = MutableStateFlow(false),
            sender = MutableStateFlow(UserInfoElement("")),
            invitation = flowOf(""),
            timelineEventFlow = timelineEventFlow,
        )

    private fun timelineEvent(content: Result<RoomEventContent>?) = TimelineEvent(
        event = MessageEvent(
            content = EncryptedMessageEventContent.MegolmEncryptedMessageEventContent(
                "",
                Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                "",
                ""
            ),
            EventId(""),
            UserId(""),
            RoomId(""),
            0L,
        ),
        content = content,
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )
}