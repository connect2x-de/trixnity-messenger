package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.util.I18n
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.defaultMessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class PrivacySettingsViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 3_000

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock1: MatrixClient

    @Mock
    lateinit var matrixClientMock2: MatrixClient

    @Mock
    lateinit var userServiceMock1: UserService

    @Mock
    lateinit var userServiceMock2: UserService

    init {
        mocker.reset()
        injectMocks(mocker)

        coroutineTestScope = true

        beforeTest {
            with(mocker) {
                every { matrixClientMock1.di } returns koinApplication {
                    modules(module { single { userServiceMock1 } })
                }.koin
                every { matrixClientMock2.di } returns koinApplication {
                    modules(module { single { userServiceMock2 } })
                }.koin
            }
        }

        should("return a list of blocked users per account") {
            with(mocker) {
                every { userServiceMock1.getAccountData<IgnoredUserListEventContent>() } returns flowOf(
                    IgnoredUserListEventContent(
                        mapOf(
                            UserId("do_not_want", "localhost") to JsonObject(emptyMap()),
                            UserId("jerk", "localhost") to JsonObject(emptyMap()),
                        )
                    )
                )
                every { userServiceMock2.getAccountData<IgnoredUserListEventContent>() } returns flowOf(
                    IgnoredUserListEventContent(
                        mapOf(
                            UserId("jerk", "localhost") to JsonObject(emptyMap()),
                            UserId("another_jerk", "localhost") to JsonObject(emptyMap()),
                        )
                    )
                )
            }
            val cut = privacySettingsViewModel(coroutineContext)
            CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) { cut.privacySettings.collect() }
            testCoroutineScheduler.advanceUntilIdle()

            val privacySettingViewModel1 = cut.privacySettings.value[0]
            privacySettingViewModel1.accountName shouldBe "test1"
            privacySettingViewModel1.presenceIsPublic.value shouldBe false
            privacySettingViewModel1.readMarkerIsPublic.value shouldBe false
            privacySettingViewModel1.typingIsPublic.value shouldBe false
            privacySettingViewModel1.blockedUsers.value shouldBe
                    listOf(
                        UserId("do_not_want", "localhost"),
                        UserId("jerk", "localhost"),
                    )

            val privacySettingViewModel2 = cut.privacySettings.value[1]
            privacySettingViewModel2.accountName shouldBe "test2"
            privacySettingViewModel2.presenceIsPublic.value shouldBe false
            privacySettingViewModel2.readMarkerIsPublic.value shouldBe false
            privacySettingViewModel2.typingIsPublic.value shouldBe false
            privacySettingViewModel2.blockedUsers.value shouldBe
                    listOf(
                        UserId("jerk", "localhost"),
                        UserId("another_jerk", "localhost"),
                    )

            cancelNeverEndingCoroutines()
        }
    }

    private fun privacySettingsViewModel(coroutineContext: CoroutineContext): PrivacySettingsViewModelImpl {
        val di = koinApplication {
            modules(
                trixnityMessengerModule(),
                testMatrixClientModule(listOf(matrixClientMock1, matrixClientMock2), listOf("test1", "test2")),
                module {
                    single<MessengerSettings> { defaultMessengerSettings("en") }
                }
            )
        }.koin
        di.get<I18n>().setCurrentLang("en")
        return PrivacySettingsViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                di = di,
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                coroutineContext = coroutineContext,
            ),
            onClosePrivacySettings = mockFunction0(mocker),
        )
    }

}