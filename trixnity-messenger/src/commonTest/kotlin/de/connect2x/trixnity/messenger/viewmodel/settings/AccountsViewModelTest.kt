package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.store.ServerData
import de.connect2x.trixnity.clientserverapi.model.media.GetMediaConfig
import de.connect2x.trixnity.clientserverapi.model.server.Capabilities
import de.connect2x.trixnity.clientserverapi.model.server.Capability
import de.connect2x.trixnity.clientserverapi.model.server.GetCapabilities
import de.connect2x.trixnity.clientserverapi.model.server.GetVersions
import de.connect2x.trixnity.clientserverapi.model.user.Profile
import de.connect2x.trixnity.clientserverapi.model.user.ProfileField
import de.connect2x.trixnity.core.ErrorResponse
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.utils.toByteArrayFlow
import dev.mokkery.answering.SuspendAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AccountsViewModelTest {
    private val ownUserId = UserId("bob", "localhost")
    private val ownUserId2 = UserId("alice", "localhost")
    private val displayName2 = "Alice"

    val matrixClientMock = mock<MatrixClient>()

    val matrixClientMock2 = mock<MatrixClient>()

    val mediaServiceMock = mock<MediaService>()

    val mediaServiceMock2 = mock<MediaService>()

    private lateinit var setDisplayNameMocker: SuspendAnsweringScope<Result<Unit>>

    init {
        resetMocks(matrixClientMock, matrixClientMock2, mediaServiceMock, mediaServiceMock2)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { mediaServiceMock }
                })
        }.koin
        val profile = Profile(ProfileField.AvatarUrl("mxc://localhost/123456"))
        every { matrixClientMock.profile } returns MutableStateFlow(profile)
        every { matrixClientMock.userId } returns ownUserId
        every { matrixClientMock.serverData } returns MutableStateFlow(
            ServerData(
                versions = GetVersions.Response(),
                mediaConfig = GetMediaConfig.Response(),
                capabilities = GetCapabilities.Response(
                    capabilities = Capabilities(
                        setOf(
                            Capability.SetDisplayName(enabled = true),
                            Capability.SetAvatarUrl(enabled = true),
                        )
                    )
                ),
            )
        )

        // mock2
        every { matrixClientMock2.di } returns koinApplication {
            modules(
                module {
                    single { mediaServiceMock2 }
                })
        }.koin

        val profile2 = Profile(ProfileField.DisplayName(displayName2), ProfileField.AvatarUrl("mxc://localhost/098765"))
        every { matrixClientMock2.profile } returns MutableStateFlow(profile2)
        everySuspend { matrixClientMock2.setProfileField(ProfileField.DisplayName()) } returns Result.success(Unit)
        every { matrixClientMock2.userId } returns ownUserId2
        everySuspend {
            mediaServiceMock2.getThumbnail(
                "mxc://localhost/098765",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar2".encodeToByteArray().toByteArrayFlow()))
        every { matrixClientMock2.serverData } returns MutableStateFlow(
            ServerData(
                versions = GetVersions.Response(),
                mediaConfig = GetMediaConfig.Response(),
                capabilities = GetCapabilities.Response(
                    capabilities = Capabilities(
                        setOf(
                            Capability.SetDisplayName(enabled = true),
                            Capability.SetAvatarUrl(enabled = true),
                        )
                    )
                ),
            )
        )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `show profiles initially`() = runTest {
        val p = Profile(
            ProfileField.DisplayName("Bob"),
            ProfileField.AvatarUrl("mxc://localhost/123456"),
        )
        every { matrixClientMock.profile } returns MutableStateFlow(p)
        everySuspend {
            mediaServiceMock.getThumbnail(
                "mxc://localhost/123456",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))

        val cut = profileViewModel()
        val accounts = cut.accountSingleViewModels
        accounts.first {
            it.size == 2 && it[0].userId == ownUserId && it[1].userId == ownUserId2
        }

        eventually(1.seconds) {
            cut.error.value shouldBe null
            val account1 = accounts.value[0]
            account1.userId shouldBe ownUserId
            account1.displayName.value shouldBe "Bob"
            account1.initials.value shouldBe "B"
            account1.avatar.value shouldBe "avatar".encodeToByteArray()
            val account2 = accounts.value[1]
            account2.userId shouldBe ownUserId2
            account2.displayName.value shouldBe "Alice"
            account2.initials.value shouldBe "A"
            account2.avatar.value shouldBe "avatar2".encodeToByteArray()
        }
    }

    @Test
    fun `set a new display name and reload profile`() = runTest {
        // do NOT move this block into the init block as it will break in iOS tests
        val profile = MutableStateFlow(Profile(ProfileField.DisplayName("Bob")))
        every { matrixClientMock.profile } returns profile
        setDisplayNameMocker = everySuspend { matrixClientMock.setProfileField(ProfileField.DisplayName(value = "Bobby")) }
        setDisplayNameMocker calls {
            profile.value += ProfileField.DisplayName((it.args[0] as ProfileField.DisplayName).value)
            Result.success(Unit)
        }
        everySuspend {
            mediaServiceMock.getThumbnail(
                "mxc://localhost/123456",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))

        val cut = profileViewModel()
        val accounts = cut.accountSingleViewModels
        accounts.first { it.size == 2 }

        eventually(1.seconds) {
            accounts.value[0].editDisplayName.textValue shouldBe "Bob"
            accounts.value[0].editDisplayName.update("Bobby")
        }

        cut.saveDisplayName(ownUserId)
        delay(200.milliseconds)
        // this leads to matrixClient.displayName to be set to "Bobby"
        verifySuspend { matrixClientMock.setProfileField(ProfileField.DisplayName("Bobby")) }
    }

    @Test
    fun `show error when new display name cannot be set`() = runTest {
        val profile = MutableStateFlow(Profile(ProfileField.DisplayName("Bob")))
        every { matrixClientMock.profile } returns profile
        setDisplayNameMocker = everySuspend { matrixClientMock.setProfileField(ProfileField.DisplayName(value = "Nobby")) }
        setDisplayNameMocker calls { Result.failure(RuntimeException("Oh no!")) }
        everySuspend {
            mediaServiceMock.getThumbnail(
                "mxc://localhost/123456",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))

        val cut = profileViewModel()
        val accounts = cut.accountSingleViewModels
        accounts.first { it.size == 2 }

        accounts.value[0].editDisplayName.update("Nobby")
        cut.saveDisplayName(ownUserId)

        eventually(1.seconds) {
            cut.error.value shouldNotBe null
            accounts.value[0].displayName.value shouldBe "Bob"
        }
    }

    @Test
    fun `display an error message when the user has not enough rights to change the display name`() = runTest {
        val profile = MutableStateFlow(Profile(ProfileField.DisplayName("Bob")))
        every { matrixClientMock.profile } returns profile
        setDisplayNameMocker = everySuspend { matrixClientMock.setProfileField(ProfileField.DisplayName(value = "Nobby")) }
        setDisplayNameMocker returns Result.failure(
            MatrixServerException(
                HttpStatusCode.Forbidden,
                ErrorResponse.Forbidden("")
            )
        )
        everySuspend {
            mediaServiceMock.getThumbnail(
                "mxc://localhost/123456",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))

        val cut = profileViewModel()
        val accounts = cut.accountSingleViewModels
        accounts.first { it.size == 2 }

        accounts.value[0].editDisplayName.update("Nobby")
        cut.saveDisplayName(ownUserId)


        eventually(1.seconds) {
            cut.error.value shouldContain "not allowed"
            accounts.value[0].displayName.value shouldBe "Bob"
        }
    }

    @Test
    fun `compute the open avatar cutter property`() = runTest {
        val profile = Profile(ProfileField.DisplayName("Bob"))
        every { matrixClientMock.profile } returns MutableStateFlow(profile)
        everySuspend {
            mediaServiceMock.getThumbnail(
                "mxc://localhost/123456",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))

        val cut = profileViewModel()
        val accounts = cut.accountSingleViewModels
        val openAvatarCutter = cut.openAvatarCutter
        accounts.first { it.size == 2 }

        openAvatarCutter.value shouldBe null
        accounts.value[1].openAvatarCutter.value = true

        eventually(1.seconds) {
            openAvatarCutter.value shouldBe ownUserId2
        }
        cut.closeAvatarCutter()
        eventually(1.seconds) {
            accounts.value[1].openAvatarCutter.value shouldBe false
            openAvatarCutter.value shouldBe null
        }
    }

    @Test
    fun `has empty display name when an explicit display name is not set`() = runTest {
        every { matrixClientMock.profile } returns MutableStateFlow(Profile())
        everySuspend {
            mediaServiceMock.getThumbnail(
                "mxc://localhost/123456",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))
        val cut = profileViewModel()
        val accounts = cut.accountSingleViewModels
        accounts.first { it.size == 2 }
        cut.accountSingleViewModels.value[0].displayName.value shouldBe ""
    }

    private fun TestScope.profileViewModel(): AccountsViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(
                        ownUserId to matrixClientMock, ownUserId2 to matrixClientMock2
                    )
                )
            )
        }.koin
        return AccountsViewModelImpl(
            viewModelContext = testViewModelContext(
                di = di,
            ),
            onCloseAccounts = mock(),
            onOpenAvatarCutter = mock(),
            onShowAccountSetup = mock(),
            onRemoveAccount = mock(),
            onCreateNewAccount = mock(),
        )
    }
}
