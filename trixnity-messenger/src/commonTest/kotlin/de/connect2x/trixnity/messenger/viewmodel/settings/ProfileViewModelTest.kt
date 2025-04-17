package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import dev.mokkery.answering.SuspendAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
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
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.client.store.ServerData
import net.folivo.trixnity.clientserverapi.model.media.GetMediaConfig
import net.folivo.trixnity.clientserverapi.model.server.Capabilities
import net.folivo.trixnity.clientserverapi.model.server.Capability
import net.folivo.trixnity.clientserverapi.model.server.GetCapabilities
import net.folivo.trixnity.clientserverapi.model.server.GetVersions
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.toByteArrayFlow
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ProfileViewModelTest {
    private val ownUserId = UserId("bob", "localhost")
    private val ownUserId2 = UserId("alice", "localhost")
    private val displayNameFlow2 = MutableStateFlow("Alice")

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
        every { matrixClientMock.avatarUrl } returns MutableStateFlow("mxc://localhost/123456")
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
        every { matrixClientMock2.displayName } returns displayNameFlow2
        everySuspend { matrixClientMock2.setDisplayName(any()) } returns Result.success(Unit)
        every { matrixClientMock2.avatarUrl } returns MutableStateFlow("mxc://localhost/098765")
        every { matrixClientMock2.userId } returns ownUserId2
        everySuspend {
            mediaServiceMock2.getThumbnail(
                eq("mxc://localhost/098765"),
                eq(avatarSize().toLong()),
                eq(avatarSize().toLong()),
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

    @Test
    fun `show profiles initially`() = runTest {
        every { matrixClientMock.displayName } returns MutableStateFlow("Bob")
        everySuspend {
            mediaServiceMock.getThumbnail(
                eq("mxc://localhost/123456"),
                eq(avatarSize().toLong()),
                eq(avatarSize().toLong()),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))

        val cut = profileViewModel()
        val profilesOfAccounts = cut.profileSingleViewModels
        profilesOfAccounts.first {
            it.size == 2 && it[0].userId == ownUserId && it[1].userId == ownUserId2
        }

        eventually(1.seconds) {
            cut.error.value shouldBe null
            val profileOfAccount = profilesOfAccounts.value[0]
            profileOfAccount.userId shouldBe ownUserId
            profileOfAccount.displayName.value shouldBe "Bob"
            profileOfAccount.initials.value shouldBe "B"
            profileOfAccount.avatar.value shouldBe "avatar".encodeToByteArray()
            val profileOfAccount2 = profilesOfAccounts.value[1]
            profileOfAccount2.userId shouldBe ownUserId2
            profileOfAccount2.displayName.value shouldBe "Alice"
            profileOfAccount2.initials.value shouldBe "A"
            profileOfAccount2.avatar.value shouldBe "avatar2".encodeToByteArray()
        }
    }

    @Test
    fun `set a new display name and reload profile`() = runTest {
        // do NOT move this block into the init block as it will break in iOS tests
        val displayNameFlow = MutableStateFlow("Bob")
        every { matrixClientMock.displayName } returns displayNameFlow
        setDisplayNameMocker = everySuspend { matrixClientMock.setDisplayName(any()) }
        setDisplayNameMocker calls {
            displayNameFlow.value = it.args[0] as String
            Result.success(Unit)
        }
        everySuspend {
            mediaServiceMock.getThumbnail(
                eq("mxc://localhost/123456"),
                eq(avatarSize().toLong()),
                eq(avatarSize().toLong()),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))

        val cut = profileViewModel()
        val profilesOfAccounts = cut.profileSingleViewModels
        profilesOfAccounts.first { it.size == 2 }

        eventually(1.seconds) {
            profilesOfAccounts.value[0].editDisplayName.textValue shouldBe "Bob"
            profilesOfAccounts.value[0].editDisplayName.update("Bobby")
        }

        cut.saveDisplayName(ownUserId)
        delay(200.milliseconds)
        // this leads to matrixClient.displayName to be set to "Bobby"
        verifySuspend { matrixClientMock.setDisplayName("Bobby") }
    }

    @Test
    fun `show error when new display name cannot be set`() = runTest {
        val displayNameFlow = MutableStateFlow("Bob")
        every { matrixClientMock.displayName } returns displayNameFlow
        setDisplayNameMocker = everySuspend { matrixClientMock.setDisplayName(any()) }
        setDisplayNameMocker calls {
            displayNameFlow.value = it.args[0] as String
            Result.success(Unit)
        }
        everySuspend {
            mediaServiceMock.getThumbnail(
                eq("mxc://localhost/123456"),
                eq(avatarSize().toLong()),
                eq(avatarSize().toLong()),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))
        setDisplayNameMocker calls { Result.failure(RuntimeException("Oh no!")) }

        val cut = profileViewModel()
        val profilesOfAccounts = cut.profileSingleViewModels
        profilesOfAccounts.first { it.size == 2 }

        profilesOfAccounts.value[0].editDisplayName.update("Nobby")
        cut.saveDisplayName(ownUserId)

        eventually(1.seconds) {
            cut.error.value shouldNotBe null
            profilesOfAccounts.value[0].displayName.value shouldBe "Bob"
        }
    }

    @Test
    fun `display an error message when the user has not enough rights to change the display name`() = runTest {
        val displayNameFlow = MutableStateFlow("Bob")
        every { matrixClientMock.displayName } returns displayNameFlow
        setDisplayNameMocker = everySuspend { matrixClientMock.setDisplayName(any()) }
        setDisplayNameMocker calls {
            displayNameFlow.value = it.args[0] as String
            Result.success(Unit)
        }
        everySuspend {
            mediaServiceMock.getThumbnail(
                eq("mxc://localhost/123456"),
                eq(avatarSize().toLong()),
                eq(avatarSize().toLong()),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))
        setDisplayNameMocker returns Result.failure(
            MatrixServerException(
                HttpStatusCode.Forbidden,
                ErrorResponse.Forbidden("")
            )
        )

        val cut = profileViewModel()
        val profilesOfAccounts = cut.profileSingleViewModels
        profilesOfAccounts.first { it.size == 2 }

        profilesOfAccounts.value[0].editDisplayName.update("Nobby")
        cut.saveDisplayName(ownUserId)


        eventually(1.seconds) {
            cut.error.value shouldContain "not allowed"
            profilesOfAccounts.value[0].displayName.value shouldBe "Bob"
        }
    }

    @Test
    fun `compute the open avatar cutter property`() = runTest {
        every { matrixClientMock.displayName } returns MutableStateFlow("Bob")
        everySuspend {
            mediaServiceMock.getThumbnail(
                eq("mxc://localhost/123456"),
                eq(avatarSize().toLong()),
                eq(avatarSize().toLong()),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))

        val cut = profileViewModel()
        val profilesOfAccounts = cut.profileSingleViewModels
        val openAvatarCutter = cut.openAvatarCutter
        profilesOfAccounts.first { it.size == 2 }

        openAvatarCutter.value shouldBe null
        profilesOfAccounts.value[1].openAvatarCutter.value = true

        eventually(1.seconds) {
            openAvatarCutter.value shouldBe ownUserId2
        }
        cut.closeAvatarCutter()
        eventually(1.seconds) {
            profilesOfAccounts.value[1].openAvatarCutter.value shouldBe false
            openAvatarCutter.value shouldBe null
        }
    }

    @Test
    fun `has empty display name when an explicit display name is not set`() = runTest {
        every { matrixClientMock.displayName } returns MutableStateFlow(null)
        everySuspend {
            mediaServiceMock.getThumbnail(
                eq("mxc://localhost/123456"),
                eq(avatarSize().toLong()),
                eq(avatarSize().toLong()),
                any(),
                any(),
                any(),
            )
        } returns Result.success(InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow()))
        val cut = profileViewModel()
        val profilesOfAccounts = cut.profileSingleViewModels
        profilesOfAccounts.first { it.size == 2 }
        cut.profileSingleViewModels.value[0].displayName.value shouldBe ""
    }

    private fun TestScope.profileViewModel(): ProfileViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(
                        ownUserId to matrixClientMock, ownUserId2 to matrixClientMock2
                    )
                )
            )
        }.koin
        return ProfileViewModelImpl(
            viewModelContext = testViewModelContext(
                di = di,
            ),
            onCloseProfile = mock(),
            onOpenAvatarCutter = mock(),
        )
    }
}
