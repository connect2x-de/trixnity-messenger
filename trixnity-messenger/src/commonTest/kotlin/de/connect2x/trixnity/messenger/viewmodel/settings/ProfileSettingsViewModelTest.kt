package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.clientserverapi.model.user.Profile
import de.connect2x.trixnity.clientserverapi.model.user.ProfileField
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerProfileSettings
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.AccountInfo
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.collections.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds


class ProfileSettingsViewModelTest {
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry()
    val matrixClientMock = mock<MatrixClient>()

    private val user = UserId("user", "server")
    val roomId = RoomId("!room1")

    val profileManagerMock = mock<ProfileManager>()

    init {
        lifecycleRegistry.resume()

        resetMocks(matrixClientMock, profileManagerMock)
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `more than one Profile and isMultiProfileEnabled enabled leads to canChangeMultiProfileMode disabled`() = runTest {
        every{profileManagerMock.profiles} returns MutableStateFlow(mapOf(
            "0" to MatrixMultiMessengerProfileSettings(delegate = mapOf(
                "displayName" to JsonPrimitive("Jerry"),
            )),
            "1" to MatrixMultiMessengerProfileSettings(delegate = mapOf(
                "displayName" to JsonPrimitive("Bob"),
            ))
        ))

        every{profileManagerMock.activeProfile} returns MutableStateFlow("0")
        every{profileManagerMock.isMultiProfileEnabled} returns MutableStateFlow(true)

        val profileSettingsViewModel = profileSettingsViewModel()
        subscribe(profileSettingsViewModel)

        delay(10)

        profileSettingsViewModel.canChangeMultiProfileMode.value shouldBe false

    }

    @Test
    fun `one Profile and isMultiProfileEnabled enabled leads to canChangeMultiProfileMode enabled`() = runTest {
        every{profileManagerMock.profiles} returns MutableStateFlow(mapOf(
            "0" to MatrixMultiMessengerProfileSettings(delegate = mapOf(
                "displayName" to JsonPrimitive("Jerry"),
            )),
        ))

        every{profileManagerMock.activeProfile} returns MutableStateFlow("0")
        every{profileManagerMock.isMultiProfileEnabled} returns MutableStateFlow(true)

        val profileSettingsViewModel = profileSettingsViewModel()
        subscribe(profileSettingsViewModel)

        delay(10)

        profileSettingsViewModel.canChangeMultiProfileMode.value shouldBe true

    }

    @Test
    fun `one Profile and isMultiProfileEnabled disabled leads to canChangeMultiProfileMode enabled`() = runTest {
        every{profileManagerMock.profiles} returns MutableStateFlow(mapOf(
            "0" to MatrixMultiMessengerProfileSettings(delegate = mapOf(
                "displayName" to JsonPrimitive("Jerry"),
            )),
        ))

        every{profileManagerMock.activeProfile} returns MutableStateFlow("0")
        every{profileManagerMock.isMultiProfileEnabled} returns MutableStateFlow(false)

        val profileSettingsViewModel = profileSettingsViewModel()
        subscribe(profileSettingsViewModel)

        delay(10)

        profileSettingsViewModel.canChangeMultiProfileMode.value shouldBe true

    }

    @Test
    fun `profile name null leads to isProfileNameSet disabled`() = runTest {
        every{profileManagerMock.profiles} returns MutableStateFlow(mapOf(
            "0" to MatrixMultiMessengerProfileSettings(delegate = mapOf(
                "displayName" to JsonNull,
            )),
        ))

        every{profileManagerMock.activeProfile} returns MutableStateFlow("0")
        every{profileManagerMock.isMultiProfileEnabled} returns MutableStateFlow(false)

        val profileSettingsViewModel = profileSettingsViewModel()
        subscribe(profileSettingsViewModel)

        delay(10)

        profileSettingsViewModel.isProfileNameSet.value shouldBe false

    }

    @Test
    fun `profile name not null leads to isProfileNameSet disabled`() = runTest {
        every{profileManagerMock.profiles} returns MutableStateFlow(mapOf(
            "0" to MatrixMultiMessengerProfileSettings(delegate = mapOf(
                "displayName" to JsonPrimitive("Jerry"),
            )),
        ))

        every{profileManagerMock.activeProfile} returns MutableStateFlow("0")
        every{profileManagerMock.isMultiProfileEnabled} returns MutableStateFlow(false)

        val profileSettingsViewModel = profileSettingsViewModel()
        subscribe(profileSettingsViewModel)

        delay(10)

        profileSettingsViewModel.isProfileNameSet.value shouldBe true

    }

    private fun TestScope.profileSettingsViewModel(): ProfilesSettingsViewModelImpl{
        val koin = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(user to matrixClientMock)) + module {
                    single { profileManagerMock }
                })
        }.koin
        koin.createScope<RootViewModelImpl>()
        return ProfilesSettingsViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                componentContext = DefaultComponentContext(lifecycleRegistry),
                di = koin,
                coroutineContext = backgroundScope.coroutineContext,
                name = "ProfileSettings"
            ),
            onCloseProfilesSettings = {}
        )
    }

    private fun TestScope.subscribe(cut: ProfilesSettingsViewModel) = backgroundScope.launch {
        launch { cut.isMultiProfile.collect(::println) }
        launch { cut.canChangeMultiProfileMode.collect(::println) }
        launch { cut.isProfileNameSet.collect(::println)}
    }
}
