package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.store.ServerData
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.UserApiClient
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AccountSingleViewModelTest {
    private val ownUserId = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val mediaServiceMock = mock<MediaService>()

    private lateinit var setDisplayNameMocker: SuspendAnsweringScope<Result<Unit>>
    private lateinit var deleteAvatarMocker: SuspendAnsweringScope<Result<Unit>>

    private val error: MutableStateFlow<String?> = MutableStateFlow(null)

    init {
        resetMocks(matrixClientMock, mediaServiceMock)
        every { matrixClientMock.di } returns koinApplication { modules(module { single { mediaServiceMock } }) }.koin
        val profile = Profile(ProfileField.AvatarUrl("mxc://localhost/123456"))
        every { matrixClientMock.profile } returns MutableStateFlow(profile)
        every { matrixClientMock.userId } returns ownUserId
        every { matrixClientMock.serverData } returns
            MutableStateFlow(
                ServerData(
                    versions = GetVersions.Response(),
                    mediaConfig = GetMediaConfig.Response(),
                    capabilities =
                        GetCapabilities.Response(
                            capabilities = Capabilities(setOf(Capability.ProfileFields(enabled = true)))
                        ),
                )
            )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `set a new display name and reload profile`() = runTest {
        // do NOT move this block into the init block as it will break in iOS tests
        val profile = MutableStateFlow(Profile(ProfileField.DisplayName("Bob")))
        every { matrixClientMock.profile } returns profile
        setDisplayNameMocker = everySuspend {
            matrixClientMock.setProfileField(ProfileField.DisplayName(value = "Bobby"))
        }
        setDisplayNameMocker calls
            {
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

        val cut = accountSingleViewModel()
        delay(200.milliseconds)
        cut.editDisplayName.textValue shouldBe "Bob"

        cut.editDisplayName.update("Bobby")
        delay(200.milliseconds)
        cut.saveDisplayName()
        delay(200.milliseconds)

        // this leads to matrixClient.displayName to be set to "Bobby"
        verifySuspend { matrixClientMock.setProfileField(ProfileField.DisplayName("Bobby")) }
    }

    @Test
    fun `show error when new display name cannot be set`() = runTest {
        val profile = MutableStateFlow(Profile(ProfileField.DisplayName("Bob")))
        every { matrixClientMock.profile } returns profile
        setDisplayNameMocker = everySuspend {
            matrixClientMock.setProfileField(ProfileField.DisplayName(value = "Nobby"))
        }
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

        val cut = accountSingleViewModel()

        cut.editDisplayName.update("Nobby")
        delay(200.milliseconds)

        cut.saveDisplayName()
        delay(200.milliseconds)

        error.value shouldNotBe null
        cut.displayName.value shouldBe "Bob"
    }

    @Test
    fun `display an error message when the user has not enough rights to change the display name`() = runTest {
        val profile = MutableStateFlow(Profile(ProfileField.DisplayName("Bob")))
        every { matrixClientMock.profile } returns profile
        setDisplayNameMocker = everySuspend {
            matrixClientMock.setProfileField(ProfileField.DisplayName(value = "Nobby"))
        }
        setDisplayNameMocker returns
            Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))
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

        val cut = accountSingleViewModel()

        cut.editDisplayName.update("Nobby")
        delay(200.milliseconds)

        cut.saveDisplayName()
        delay(200.milliseconds)

        error.value shouldContain "not allowed"
        cut.displayName.value shouldBe "Bob"
    }

    @Test
    fun `delete avatar image and reload profile`() = runTest {
        val profile = MutableStateFlow(Profile(ProfileField.AvatarUrl("mxc://localhost/123456")))
        every { matrixClientMock.profile } returns profile
        deleteAvatarMocker = everySuspend {
            matrixClientMock.deleteProfileField(ProfileField.AvatarUrl)
        }
        deleteAvatarMocker calls {
            profile.value -= ProfileField.AvatarUrl
            Result.success(Unit)
        }

        val initialAvatar = InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow())
        val initialAvatarValue = initialAvatar.toByteArray()
        everySuspend {
            mediaServiceMock.getThumbnail(
                "mxc://localhost/123456",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(initialAvatar)

        val cut = accountSingleViewModel()
        backgroundScope.launch { cut.canDeleteAvatar.collect {} }
        backgroundScope.launch { cut.hasAvatarUrl.collect {} }

        delay(200.milliseconds)
        cut.avatar.value shouldBe initialAvatarValue

        cut.deleteAvatar()
        delay(200.milliseconds)

        // after successful deletion avatar must be null
        verifySuspend { matrixClientMock.deleteProfileField(ProfileField.AvatarUrl) }
        cut.avatar.value shouldBe null
    }

    @Test
    fun `show error when avatar image cannot be deleted`() = runTest {
        val profile = MutableStateFlow(Profile(ProfileField.AvatarUrl("mxc://localhost/123456")))
        every { matrixClientMock.profile } returns profile
        deleteAvatarMocker = everySuspend {
            matrixClientMock.deleteProfileField(ProfileField.AvatarUrl)
        }
        deleteAvatarMocker returns
                Result.failure(RuntimeException("Something went wrong!"))

        val initialAvatar = InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow())
        val initialAvatarValue = initialAvatar.toByteArray()
        everySuspend {
            mediaServiceMock.getThumbnail(
                "mxc://localhost/123456",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(initialAvatar)

        val cut = accountSingleViewModel()
        backgroundScope.launch { cut.canDeleteAvatar.collect {} }
        backgroundScope.launch { cut.hasAvatarUrl.collect {} }

        delay(200.milliseconds)
        cut.avatar.value shouldBe initialAvatarValue

        cut.deleteAvatar()
        delay(200.milliseconds)

        error.value shouldNotBe null
        cut.avatar.value shouldBe initialAvatarValue
    }

    @Test
    fun `display an error message when the user has not enough rights to delete the avatar image`() = runTest {
        val profile = MutableStateFlow(Profile(ProfileField.AvatarUrl("mxc://localhost/123456")))
        every { matrixClientMock.profile } returns profile
        deleteAvatarMocker = everySuspend {
            matrixClientMock.deleteProfileField(ProfileField.AvatarUrl)
        }
        deleteAvatarMocker returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("You shall not pass")))

        val initialAvatar = InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow())
        val initialAvatarValue = initialAvatar.toByteArray()
        everySuspend {
            mediaServiceMock.getThumbnail(
                "mxc://localhost/123456",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(initialAvatar)

        val cut = accountSingleViewModel()
        backgroundScope.launch { cut.canDeleteAvatar.collect {} }
        backgroundScope.launch { cut.hasAvatarUrl.collect {} }

        delay(200.milliseconds)
        cut.avatar.value shouldBe initialAvatarValue

        cut.deleteAvatar()
        delay(200.milliseconds)

        error.value shouldContain "not allowed"
        cut.avatar.value shouldBe initialAvatarValue
    }

    @Test
    fun `don't delete avatar image when capabilities are missing`() = runTest {
        val capabilities = MutableStateFlow(
            ServerData(
                versions = GetVersions.Response(),
                mediaConfig = GetMediaConfig.Response(),
                capabilities = GetCapabilities.Response(
                    capabilities = Capabilities(
                        setOf(
                            Capability.ProfileFields(enabled = false),
                            Capability.SetAvatarUrl(enabled = false),
                        )
                    )
                ),
            )
        )
        every { matrixClientMock.serverData } returns capabilities
        val profile = MutableStateFlow(Profile(ProfileField.AvatarUrl("mxc://localhost/123456")))
        every { matrixClientMock.profile } returns profile
        deleteAvatarMocker = everySuspend {
            matrixClientMock.deleteProfileField(ProfileField.AvatarUrl)
        }
        deleteAvatarMocker calls {
            profile.value -= ProfileField.AvatarUrl
            Result.success(Unit)
        }

        val initialAvatar = InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow())
        val initialAvatarValue = initialAvatar.toByteArray()
        everySuspend {
            mediaServiceMock.getThumbnail(
                "mxc://localhost/123456",
                avatarSize().toLong(),
                avatarSize().toLong(),
                any(),
                any(),
                any(),
            )
        } returns Result.success(initialAvatar)

        val cut = accountSingleViewModel()
        backgroundScope.launch { cut.canDeleteAvatar.collect {} }
        backgroundScope.launch { cut.hasAvatarUrl.collect {} }

        delay(200.milliseconds)
        cut.avatar.value shouldBe initialAvatarValue

        cut.deleteAvatar()
        delay(200.milliseconds)

        cut.avatar.value shouldBe initialAvatarValue
    }

    private fun TestScope.accountSingleViewModel(): AccountSingleViewModelImpl {
        val di =
            koinApplication { modules(createTestDefaultTrixnityMessengerModules(mapOf(ownUserId to matrixClientMock))) }
                .koin
        return AccountSingleViewModelImpl(
            viewModelContext = testViewModelContext(di = di),
            userId = ownUserId,
            error = error,
            showAccountSetup = mock(),
            removeAccount = mock(),
        )
    }
}
