package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.util.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.mock.MediaServiceMock
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.toByteArrayFlow
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction2
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    private val ownUserId = UserId("bob", "localhost")
    private val ownUserId2 = UserId("alice", "localhost")
    private val displayNameFlow2 = MutableStateFlow("Alice")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var matrixClientMock2: MatrixClient

    @Mock
    lateinit var mediaServiceMock: MediaService

    @Mock
    lateinit var mediaServiceMock2: MediaService

    private lateinit var setDisplayNameMocker: Mocker.EverySuspend<Result<Unit>>

    init {
        Dispatchers.setMain(testMainDispatcher)

        beforeTest {
            mocker.reset()
            injectMocks(mocker)
            mediaServiceMock = MediaServiceMock(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { mediaServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.avatarUrl } returns MutableStateFlow("mxc://localhost/123456")
                every { matrixClientMock.userId } returns ownUserId

                // mock2
                every { matrixClientMock2.di } returns koinApplication {
                    modules(
                        module {
                            single { mediaServiceMock2 }
                        }
                    )
                }.koin
                every { matrixClientMock2.displayName } returns displayNameFlow2
                everySuspending { matrixClientMock2.setDisplayName(isAny()) } returns Result.success(Unit)
                every { matrixClientMock2.avatarUrl } returns MutableStateFlow("mxc://localhost/098765")
                every { matrixClientMock2.userId } returns ownUserId2
                mocker.everySuspending {
                    mediaServiceMock2.getThumbnail(
                        isEqual("mxc://localhost/098765"),
                        isEqual(avatarSize().toLong()),
                        isEqual(avatarSize().toLong()),
                        isAny(),
                        isAny(),
                        isAny(),
                    )
                } returns Result.success("avatar2".encodeToByteArray().toByteArrayFlow())
            }
        }

        should("show profiles initially") {
            mocker.every { matrixClientMock.displayName } returns MutableStateFlow("Bob")
            mocker.everySuspending {
                mediaServiceMock.getThumbnail(
                    isEqual("mxc://localhost/123456"),
                    isEqual(avatarSize().toLong()),
                    isEqual(avatarSize().toLong()),
                    isAny(),
                    isAny(),
                    isAny(),
                )
            } returns Result.success("avatar".encodeToByteArray().toByteArrayFlow())

            val cut = profileViewModel()
            val profilesOfAccounts = cut.profilesOfAccounts
            profilesOfAccounts.first {
                it.size == 2 &&
                        it[0].accountName == "test" && it[1].accountName == "test2"
            }

            eventually(1.seconds) {
                cut.error.value shouldBe null
                val profileOfAccount = profilesOfAccounts.value[0]
                profileOfAccount.userId shouldBe ownUserId.full
                profileOfAccount.displayName.value shouldBe "Bob"
                profileOfAccount.initials.value shouldBe "B"
                profileOfAccount.avatar.value shouldBe "avatar".encodeToByteArray()
                val profileOfAccount2 = profilesOfAccounts.value[1]
                profileOfAccount2.userId shouldBe ownUserId2.full
                profileOfAccount2.displayName.value shouldBe "Alice"
                profileOfAccount2.initials.value shouldBe "A"
                profileOfAccount2.avatar.value shouldBe "avatar2".encodeToByteArray()
            }
        }

        should("set a new display name and reload profile") {
            // do NOT move this block into the init block as it will break in iOS tests
            val displayNameFlow = MutableStateFlow("Bob")
            mocker.every { matrixClientMock.displayName } returns displayNameFlow
            setDisplayNameMocker = mocker.everySuspending { matrixClientMock.setDisplayName(isAny()) }
            setDisplayNameMocker runs {
                displayNameFlow.value = it[0] as String
                Result.success(Unit)
            }
            mocker.everySuspending {
                mediaServiceMock.getThumbnail(
                    isEqual("mxc://localhost/123456"),
                    isEqual(avatarSize().toLong()),
                    isEqual(avatarSize().toLong()),
                    isAny(),
                    isAny(),
                    isAny(),
                )
            } returns Result.success("avatar".encodeToByteArray().toByteArrayFlow())

            val cut = profileViewModel()
            val profilesOfAccounts = cut.profilesOfAccounts
            profilesOfAccounts.first { it.size == 2 }

            eventually(1.seconds) {
                profilesOfAccounts.value[0].editDisplayName.value shouldBe "Bob"
                profilesOfAccounts.value[0].editDisplayName.value = "Bobby"
            }

            cut.saveDisplayName("test")
            delay(200.milliseconds)
            // this leads to matrixClient.displayName to be set to "Bobby"
            mocker.verifyWithSuspend(exhaustive = false) { matrixClientMock.setDisplayName("Bobby") }
        }

        should("show error when new display name cannot be set") {
            val displayNameFlow = MutableStateFlow("Bob")
            mocker.every { matrixClientMock.displayName } returns displayNameFlow
            setDisplayNameMocker = mocker.everySuspending { matrixClientMock.setDisplayName(isAny()) }
            setDisplayNameMocker runs {
                displayNameFlow.value = it[0] as String
                Result.success(Unit)
            }
            mocker.everySuspending {
                mediaServiceMock.getThumbnail(
                    isEqual("mxc://localhost/123456"),
                    isEqual(avatarSize().toLong()),
                    isEqual(avatarSize().toLong()),
                    isAny(),
                    isAny(),
                    isAny(),
                )
            } returns Result.success("avatar".encodeToByteArray().toByteArrayFlow())
            setDisplayNameMocker runs { Result.failure(RuntimeException("Oh no!")) }

            val cut = profileViewModel()
            val profilesOfAccounts = cut.profilesOfAccounts
            profilesOfAccounts.first { it.size == 2 }

            profilesOfAccounts.value[0].editDisplayName.value = "Nobby"
            cut.saveDisplayName("test")

            eventually(1.seconds) {
                cut.error.value shouldNotBe null
                profilesOfAccounts.value[0].displayName.value shouldBe "Bob"
            }
        }

        should("display an error message when the user has not enough rights to change the display name") {
            val displayNameFlow = MutableStateFlow("Bob")
            mocker.every { matrixClientMock.displayName } returns displayNameFlow
            setDisplayNameMocker = mocker.everySuspending { matrixClientMock.setDisplayName(isAny()) }
            setDisplayNameMocker runs {
                displayNameFlow.value = it[0] as String
                Result.success(Unit)
            }
            mocker.everySuspending {
                mediaServiceMock.getThumbnail(
                    isEqual("mxc://localhost/123456"),
                    isEqual(avatarSize().toLong()),
                    isEqual(avatarSize().toLong()),
                    isAny(),
                    isAny(),
                    isAny(),
                )
            } returns Result.success("avatar".encodeToByteArray().toByteArrayFlow())
            setDisplayNameMocker returns
                    Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden()))

            val cut = profileViewModel()
            val profilesOfAccounts = cut.profilesOfAccounts
            profilesOfAccounts.first { it.size == 2 }

            profilesOfAccounts.value[0].editDisplayName.value = "Nobby"
            cut.saveDisplayName("test")


            eventually(1.seconds) {
                cut.error.value shouldContain "not allowed"
                profilesOfAccounts.value[0].displayName.value shouldBe "Bob"
            }
        }

        should("compute the open avatar cutter property") {
            mocker.every { matrixClientMock.displayName } returns MutableStateFlow("Bob")
            mocker.everySuspending {
                mediaServiceMock.getThumbnail(
                    isEqual("mxc://localhost/123456"),
                    isEqual(avatarSize().toLong()),
                    isEqual(avatarSize().toLong()),
                    isAny(),
                    isAny(),
                    isAny(),
                )
            } returns Result.success("avatar".encodeToByteArray().toByteArrayFlow())

            val cut = profileViewModel()
            val profilesOfAccounts = cut.profilesOfAccounts
            val openAvatarCutter = cut.openAvatarCutter
            profilesOfAccounts.first { it.size == 2 }

            openAvatarCutter.value shouldBe null
            profilesOfAccounts.value[1].openAvatarCutter.value = true

            eventually(1.seconds) {
                openAvatarCutter.value shouldBe "test2"
            }
            cut.closeAvatarCutter()
            eventually(1.seconds) {
                profilesOfAccounts.value[1].openAvatarCutter.value shouldBe false
                openAvatarCutter.value shouldBe null
            }
        }
    }

    private fun profileViewModel(): ProfileViewModelImpl {
        val di = koinApplication {
            modules(
                trixnityMessengerModule(),
                testMatrixClientModule(listOf(matrixClientMock, matrixClientMock2), listOf("test", "test2"))
            )
        }.koin
        di.get<I18n>().setCurrentLang("en")
        return ProfileViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                accountName = "test",
            ),
            onCloseProfile = mockFunction0(mocker),
            onOpenAvatarCutter = mockFunction2(mocker),
        )
    }
}
