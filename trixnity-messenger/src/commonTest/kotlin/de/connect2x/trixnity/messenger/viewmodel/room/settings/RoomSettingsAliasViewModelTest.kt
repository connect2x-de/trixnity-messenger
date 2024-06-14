package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

val log = KotlinLogging.logger("RoomSettingsAliasViewModel Test")

class RoomSettingsAliasViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 10_000

    val mocker = Mocker()

    private val roomId = RoomId("room", "127.0.0.1")
    private val me = UserId("user", "127.0.0.1")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomApiClient

    private val canSendEvent = MutableStateFlow(true)
    private val serverAliases = MutableStateFlow<CanonicalAliasEventContent?>(null)

    init {
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            serverAliases.value = null
            canSendEvent.value = true

            with(mocker) {
                every {
                    roomServiceMock.getState(isAny(), isEqual(CanonicalAliasEventContent::class), isAny())
                } returns serverAliases.map {
                    if (it == null) null
                    else ClientEvent.RoomEvent.StateEvent(
                        content = it,
                        roomId = roomId,
                        stateKey = "",
                        id = EventId("eventId"),
                        sender = me,
                        unsigned = null,
                        originTimestamp = 1234
                    )
                }

                everySuspending {
                    roomsApiClientMock.sendStateEvent(isAny(), isAny(), isAny(), isAny())
                } runs {
                    serverAliases.value = it[1] as CanonicalAliasEventContent
                    Result.success(EventId(""))
                }

                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.userId } returns me
                every { matrixClientMock.api } returns matrixClientServerApiMock
                every { matrixClientServerApiMock.room } returns roomsApiClientMock

                mocker.every {
                    userServiceMock.canSendEvent(isAny(), isEqual(CanonicalAliasEventContent::class))
                } returns canSendEvent
            }
        }

        should("load permissions to change the room alias based on the user's power level") {
            withTestingHarness {
                val viewModel = roomSettingsAliasViewModel(coroutineContext)

                eventually(2.seconds) {
                    viewModel.canChangeRoomAlias.value shouldBe true
                }

                canSendEvent.value = false
                eventually(2.seconds) {
                    viewModel.canChangeRoomAlias.value shouldBe false
                }
            }
        }

        should("add alias") {
            withTestingHarness {
                val error = MutableStateFlow<String?>(null)
                val viewModel = roomSettingsAliasViewModel(coroutineContext, addError = error)

                eventually(2.seconds) {
                    viewModel.canChangeRoomAlias.value shouldBe true
                }

                viewModel.newAlias.value = "#epicroom:127.0.0.1"
                viewModel.addNewAlias()

                eventually(2.seconds) {
                    viewModel.isUpdating.value shouldBe false
                    error.value shouldBe null
                }

                mocker.verifyWithSuspend(exhaustive = false) {
                    roomsApiClientMock.sendStateEvent(
                        isAny(), isEqual(
                            CanonicalAliasEventContent(
                                null,
                                setOf(RoomAliasId("#epicroom:127.0.0.1"))
                            )
                        ), isAny(), isAny()
                    )
                }
            }
        }

        should("not add invalid alias") {
            withTestingHarness {
                val error = MutableStateFlow<String?>(null)
                val viewModel = roomSettingsAliasViewModel(coroutineContext, addError = error)

                eventually(2.seconds) {
                    viewModel.canChangeRoomAlias.value shouldBe true
                }

                viewModel.newAlias.value = "epicroom:127.0.0.1"
                viewModel.addNewAlias()

                eventually(2.seconds) {
                    viewModel.isUpdating.value shouldBe false
                    error.value shouldBe viewModel.i18n.settingsRoomAliasAddAliasInvalid()
                }
            }
        }

        should("set main alias") {
            withTestingHarness {
                val error = MutableStateFlow<String?>(null)
                val viewModel = roomSettingsAliasViewModel(coroutineContext, addError = error)

                eventually(2.seconds) {
                    viewModel.canChangeRoomAlias.value shouldBe true
                }

                viewModel.newAlias.value = "#epicroom:127.0.0.1"
                viewModel.addNewAlias()

                eventually(2.seconds) {
                    viewModel.isUpdating.value shouldBe false
                    error.value shouldBe null
                    viewModel.moreAliases.value shouldContain "#epicroom:127.0.0.1"
                }

                log.debug { serverAliases.value?.aliases }
                viewModel.changeMainAlias(RoomAliasId("#epicroom:127.0.0.1"))

                eventually(2.seconds) {
                    viewModel.isUpdating.value shouldBe false
                    error.value shouldBe null
                }

                mocker.verifyWithSuspend(exhaustive = false) {
                    roomsApiClientMock.sendStateEvent(
                        isAny(), isEqual(
                            CanonicalAliasEventContent(
                                RoomAliasId("#epicroom:127.0.0.1"),
                                setOf<RoomAliasId>()
                            )
                        ), isAny(), isAny()
                    )
                }
            }
        }
    }

    private suspend fun TestScope.withTestingHarness(testFn: suspend TestScope.() -> Unit) {
        mocker.every {
            // mockmp requires us to mock the user service within each test.
            userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
        } returns MutableStateFlow(null)

        testFn(this)
        cancelNeverEndingCoroutines()
    }

    private fun roomSettingsAliasViewModel(
        coroutineContext: CoroutineContext,
        removeError: MutableStateFlow<String?> = MutableStateFlow(null),
        updateError: MutableStateFlow<String?> = MutableStateFlow(null),
        addError: MutableStateFlow<String?> = MutableStateFlow(null),
    ): RoomSettingsAliasViewModelImpl {
        return RoomSettingsAliasViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext,
            ),
            selectedRoomId = roomId,
            removeAliasError = removeError,
            updateError = updateError,
            newAliasError = addError
        )
    }
}
