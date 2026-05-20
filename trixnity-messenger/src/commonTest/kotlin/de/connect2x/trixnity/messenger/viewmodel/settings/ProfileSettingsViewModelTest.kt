package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerProfileSettings
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.koin.dsl.koinApplication
import org.koin.dsl.module

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
    fun `more than one Profile and isMultiProfileEnabled enabled leads to canChangeMultiProfileMode disabled`() =
        runTest {
            every { profileManagerMock.profiles } returns
                MutableStateFlow(
                    mapOf(
                        "0" to
                            MatrixMultiMessengerProfileSettings(
                                delegate = mapOf("displayName" to JsonPrimitive("Jerry"))
                            ),
                        "1" to
                            MatrixMultiMessengerProfileSettings(delegate = mapOf("displayName" to JsonPrimitive("Bob"))),
                    )
                )

            every { profileManagerMock.activeProfile } returns MutableStateFlow("0")
            every { profileManagerMock.isMultiProfileEnabled } returns MutableStateFlow(true)

            val profileSettingsViewModel = profileSettingsViewModel()
            subscribe(profileSettingsViewModel)

            delay(10)

            profileSettingsViewModel.canChangeMultiProfileMode.value shouldBe false
        }

    @Test
    fun `one Profile and isMultiProfileEnabled enabled leads to canChangeMultiProfileMode enabled`() = runTest {
        every { profileManagerMock.profiles } returns
            MutableStateFlow(
                mapOf(
                    "0" to
                        MatrixMultiMessengerProfileSettings(delegate = mapOf("displayName" to JsonPrimitive("Jerry")))
                )
            )

        every { profileManagerMock.activeProfile } returns MutableStateFlow("0")
        every { profileManagerMock.isMultiProfileEnabled } returns MutableStateFlow(true)

        val profileSettingsViewModel = profileSettingsViewModel()
        subscribe(profileSettingsViewModel)

        delay(10)

        profileSettingsViewModel.canChangeMultiProfileMode.value shouldBe true
    }

    @Test
    fun `one Profile and isMultiProfileEnabled disabled leads to canChangeMultiProfileMode enabled`() = runTest {
        every { profileManagerMock.profiles } returns
            MutableStateFlow(
                mapOf(
                    "0" to
                        MatrixMultiMessengerProfileSettings(delegate = mapOf("displayName" to JsonPrimitive("Jerry")))
                )
            )

        every { profileManagerMock.activeProfile } returns MutableStateFlow("0")
        every { profileManagerMock.isMultiProfileEnabled } returns MutableStateFlow(false)

        val profileSettingsViewModel = profileSettingsViewModel()
        subscribe(profileSettingsViewModel)

        delay(10)

        profileSettingsViewModel.canChangeMultiProfileMode.value shouldBe true
    }

    @Test
    fun `ProfilesSettingsSingleViewModel is created correctly`() = runTest {
        every { profileManagerMock.profiles } returns
            MutableStateFlow(
                mapOf(
                    "0" to
                        MatrixMultiMessengerProfileSettings(delegate = mapOf("displayName" to JsonPrimitive("Jerry"))),
                    "1" to
                        MatrixMultiMessengerProfileSettings(delegate = mapOf("displayName" to JsonPrimitive("Gustav"))),
                )
            )

        every { profileManagerMock.activeProfile } returns MutableStateFlow("0")
        every { profileManagerMock.isMultiProfileEnabled } returns MutableStateFlow(true)

        val profileSettingsViewModel = profileSettingsViewModel()
        subscribe(profileSettingsViewModel)

        delay(10)

        val profile1 = profileSettingsViewModel.profiles.value["0"]
        val profile2 = profileSettingsViewModel.profiles.value["1"]

        delay(10)

        requireNotNull(profile1)
        requireNotNull(profile2)

        subscribe(profile1)
        subscribe(profile2)

        eventually(1.seconds) {
            profile1.profileId shouldBe "0"
            profile1.profileName.value shouldBe "Jerry"
            profile2.profileId shouldBe "1"
            profile2.profileName.value shouldBe "Gustav"
        }
    }

    @Test
    fun `ProfileCreationTextField text value equal to an existing profile should lead to profileCreationError being not null`() =
        runTest {
            every { profileManagerMock.profiles } returns
                MutableStateFlow(
                    mapOf(
                        "0" to
                            MatrixMultiMessengerProfileSettings(
                                delegate = mapOf("displayName" to JsonPrimitive("Jerry"))
                            ),
                        "1" to
                            MatrixMultiMessengerProfileSettings(
                                delegate = mapOf("displayName" to JsonPrimitive("Gustav"))
                            ),
                    )
                )

            every { profileManagerMock.activeProfile } returns MutableStateFlow("0")
            every { profileManagerMock.isMultiProfileEnabled } returns MutableStateFlow(true)

            val profileSettingsViewModel = profileSettingsViewModel()
            subscribe(profileSettingsViewModel)

            profileSettingsViewModel.profileCreationTextField.update("Gustav")
            eventually(1.seconds) { (profileSettingsViewModel.profileCreationError.value == null) shouldBe false }

            profileSettingsViewModel.profileCreationTextField.update("Jerry")
            eventually(1.seconds) { (profileSettingsViewModel.profileCreationError.value == null) shouldBe false }
        }

    @Test
    fun `ProfilesSettingsSingleViewModel profileNameError is not null when new name for renaming already exists except for the active profile`() =
        runTest {
            every { profileManagerMock.profiles } returns
                MutableStateFlow(
                    mapOf(
                        "0" to
                            MatrixMultiMessengerProfileSettings(
                                delegate = mapOf("displayName" to JsonPrimitive("Jerry"))
                            ),
                        "1" to
                            MatrixMultiMessengerProfileSettings(
                                delegate = mapOf("displayName" to JsonPrimitive("Gustav"))
                            ),
                    )
                )
            every { profileManagerMock.activeProfile } returns MutableStateFlow("0")
            every { profileManagerMock.isMultiProfileEnabled } returns MutableStateFlow(true)

            val singleViewModel = profileSettingsSingleViewModel("0")
            subscribe(singleViewModel)

            singleViewModel.profileNameTextField.update("Gustav")
            eventually(1.seconds) { requireNotNull(singleViewModel.profileNameError.value) }

            singleViewModel.profileNameTextField.update("Jerry")
            eventually(1.seconds) { require(singleViewModel.profileNameError.value == null) }
        }

    private fun TestScope.profileSettingsViewModel(): ProfilesSettingsViewModelImpl {
        val koin =
            koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(user to matrixClientMock)) +
                            module { single { profileManagerMock } }
                    )
                }
                .koin
        koin.createScope<RootViewModelImpl>()
        return ProfilesSettingsViewModelImpl(
            viewModelContext =
                ViewModelContextImpl(
                    componentContext = DefaultComponentContext(lifecycleRegistry),
                    di = koin,
                    coroutineContext = backgroundScope.coroutineContext,
                    name = "ProfileSettings",
                ),
            onCloseProfilesSettings = {},
        )
    }

    private fun TestScope.profileSettingsSingleViewModel(profileId: String): ProfilesSettingsSingleViewModelImpl {
        val koin =
            koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(user to matrixClientMock)) +
                            module { single { profileManagerMock } }
                    )
                }
                .koin
        koin.createScope<RootViewModelImpl>()
        return ProfilesSettingsSingleViewModelImpl(
            viewModelContext =
                ViewModelContextImpl(
                    componentContext = DefaultComponentContext(lifecycleRegistry),
                    di = koin,
                    coroutineContext = backgroundScope.coroutineContext,
                    name = "ProfileSettingsSingle",
                ),
            profileId = profileId,
        )
    }

    private fun TestScope.subscribe(cut: ProfilesSettingsViewModel) = backgroundScope.launch {
        launch { cut.isMultiProfile.collect(::println) }
        launch { cut.canChangeMultiProfileMode.collect(::println) }
        launch { cut.profiles.collect(::println) }
        launch { cut.profileCreationError.collect(::println) }
        launch { cut.profileCreationTextField.collect(::println) }
        launch { cut.activeProfile.collect(::println) }
    }

    private fun TestScope.subscribe(cut: ProfilesSettingsSingleViewModel) = backgroundScope.launch {
        launch { cut.profileName.collect(::println) }
        launch { cut.profileNameTextField.collect(::println) }
        launch { cut.profileNameError.collect(::println) }
    }
}
