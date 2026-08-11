package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.update
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication

class InitialSettingsSetupViewModelTest {
    private val userId = UserId("@me:local.host")
    private val notificationsViewModelMock = mock<NotificationSettingsSingleAccountViewModel>()

    private val messengerSettings: MatrixMessengerSettingsHolder = createTestMatrixMessengerSettingsHolder()

    @BeforeTest
    fun setup() {
        every { notificationsViewModelMock.enabledForThisDevice } returns MutableStateFlow(true)
        every { notificationsViewModelMock.availableProviders } returns emptyList()
        every { notificationsViewModelMock.selectedProvider } returns MutableStateFlow(null)
        every { notificationsViewModelMock.notificationHandlerId } returns "id"
        every { notificationsViewModelMock.notificationPermissionsNecessary } returns MutableStateFlow(false)
    }

    @Test
    fun `strict privacy should be false if settings cannot be read`() = runTest {
        val cut = createInitialSettingsSetupViewModel(null)
        delay(100.milliseconds)
        cut.strictPrivacyEnabled.value shouldBe false
    }

    @Test
    fun `strict privacy should be false if any privacy setting is not strict`() = runTest {
        val settings = MatrixMessengerAccountSettingsBase()
        val cut = createInitialSettingsSetupViewModel(settings)
        messengerSettings.update<MatrixMessengerAccountSettingsBase>(userId) {
            it.copy(presenceIsPublic = true, typingIsPublic = false, readMarkerIsPublic = false)
        }
        delay(100.milliseconds)
        cut.strictPrivacyEnabled.value shouldBe false
    }

    @Test
    fun `strict privacy should be true if all privacy settings are strict`() = runTest {
        val settings = MatrixMessengerAccountSettingsBase()
        val cut = createInitialSettingsSetupViewModel(settings)
        messengerSettings.update<MatrixMessengerAccountSettingsBase>(userId) {
            it.copy(presenceIsPublic = false, typingIsPublic = false, readMarkerIsPublic = false)
        }
        delay(100.milliseconds)
        cut.strictPrivacyEnabled.value shouldBe true
    }

    @Test
    fun `toggle strict privacy should set all privacy values to the opposite`() = runTest {
        val settings = MatrixMessengerAccountSettingsBase()
        val cut = createInitialSettingsSetupViewModel(settings)
        messengerSettings.update<MatrixMessengerAccountSettingsBase>(userId) {
            it.copy(presenceIsPublic = true, typingIsPublic = false, readMarkerIsPublic = false)
        }
        delay(100.milliseconds)
        cut.toggleStrictPrivacy()
        delay(100.milliseconds)
        cut.strictPrivacyEnabled.value shouldBe true
        cut.toggleStrictPrivacy()
        delay(100.milliseconds)
        cut.strictPrivacyEnabled.value shouldBe false
    }

    private suspend fun TestScope.createInitialSettingsSetupViewModel(
        settings: MatrixMessengerAccountSettingsBase?
    ): InitialSettingsSetupViewModel {
        settings?.let { messengerSettings.create(userId, settings) }

        return InitialSettingsSetupViewModelImpl(
            viewModelContext =
                testMatrixClientViewModelContext(
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        matrixClients = emptyMap(),
                                        settings = messengerSettings,
                                    )
                                )
                            }
                            .koin,
                    userId = userId,
                ),
            notificationsSettingsSingleAccountViewModel = notificationsViewModelMock,
        )
    }
}
