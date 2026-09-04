package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.client.ModuleFactory
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.createTestMatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldNotBe
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.koin.dsl.module

class MultiMessengerProfilesIT {
    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    val matrixClientsMock = mock<MatrixClients>()

    @Test
    fun `should handle creation and selection of multiple profiles correctly`() = runTest {
        val multiMessenger = createTestMatrixMultiMessenger(coroutineContext = backgroundScope.coroutineContext)
        val profile1 = multiMessenger.createProfile()
        val profile2 = multiMessenger.createProfile()

        delay(1.seconds)
        multiMessenger.profiles.value shouldHaveSize 2
        multiMessenger.selectProfile(profile1)
        delay(1.seconds)
        val matrixMessenger1 = multiMessenger.activeMatrixMessenger.value shouldNotBe null

        multiMessenger.selectProfile(profile2)
        delay(1.seconds)
        val matrixMessenger2 = multiMessenger.activeMatrixMessenger.value shouldNotBe null

        matrixMessenger1 shouldNotBe matrixMessenger2

        matrixMessenger2?.close()
    }

    @Test
    fun `should initiate logout of all accounts associated with a profile on deletion`() = runTest {
        everySuspend { matrixClientsMock.logoutAll() } returns emptyMap()
        every { matrixClientsMock.isInitialized } returns MutableStateFlow(true)
        everySuspend { matrixClientsMock.collect(any()) } calls
            { (collector: FlowCollector<Map<UserId, MatrixMessenger>>) ->
                MutableStateFlow<Map<UserId, MatrixMessenger>>(emptyMap()).collect(collector)
            }

        val multiMessenger =
            createTestMatrixMultiMessenger(
                coroutineContext = backgroundScope.coroutineContext,
                listOf { module { single<MatrixClients> { matrixClientsMock } } },
            )
        val profile1 = multiMessenger.createProfile()
        val profile2 = multiMessenger.createProfile()

        delay(1.seconds)
        multiMessenger.profiles.value shouldHaveSize 2
        multiMessenger.selectProfile(profile1)

        // Delete non active profile
        delay(1.seconds)
        multiMessenger.activeMatrixMessenger.value shouldNotBe null
        multiMessenger.deleteProfile(profile2)
        delay(1.seconds)

        // Delete active profile
        multiMessenger.deleteProfile(profile1)

        delay(1.seconds)
        multiMessenger.activeMatrixMessenger.value.shouldBeNull()
        multiMessenger.activeProfile.value.shouldBeNull()

        verifySuspend(exactly(2)) { matrixClientsMock.logoutAll() }
    }

    suspend fun TestScope.createTestMatrixMultiMessenger(
        coroutineContext: CoroutineContext = Dispatchers.Default,
        additionalMessengerModules: List<ModuleFactory> = emptyList(),
    ) =
        MatrixMultiMessengerImpl(coroutineContext) {
            messengerConfiguration {
                modulesFactories +=
                    createTestDefaultTrixnityMessengerModules().map { { it } } + additionalMessengerModules
            }
            modulesFactories =
                listOf {
                    module {
                        // TODO there should be a more clean way for I18n
                        single<I18n> {
                            object :
                                I18n(
                                    DefaultLanguages,
                                    createTestMatrixMessengerSettingsHolder(),
                                    GetSystemLang { "en" },
                                    TimeZone.of("CET"),
                                ) {}
                        }
                        // TODO this needs to be removed and fixed, as there is no MatrixMessengerSettingsHolderImpl at
                        // MultiMessenger level!
                        single<MatrixMessengerSettingsHolder> { createTestMatrixMessengerSettingsHolder() }
                    }
                } +
                    createTrixnityMultiMessengerDefaultModuleFactories() +
                    {
                        module {
                            single<MatrixMultiMessengerSettingsHolder> {
                                createTestMatrixMultiMessengerSettingsHolder()
                            }
                        }
                    }
        }
}
