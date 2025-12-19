package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.createTestMatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MultiMessengerProfilesIT {
    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun shouldHandleMultipleProfiles() = runTest {
        val multiMessenger = createTestMatrixMultiMessenger(
            coroutineContext = backgroundScope.coroutineContext
        )
        val profile1 = multiMessenger.createProfile()
        val profile2 = multiMessenger.createProfile()

        advanceTimeBy(1.seconds)
        multiMessenger.profiles.value shouldHaveSize 2
        multiMessenger.selectProfile(profile1)
        advanceTimeBy(1.seconds)
        val matrixMessenger1 = multiMessenger.activeMatrixMessenger.value shouldNotBe null

        multiMessenger.selectProfile(profile2)
        advanceTimeBy(1.seconds)
        val matrixMessenger2 = multiMessenger.activeMatrixMessenger.value shouldNotBe null

        matrixMessenger1 shouldNotBe matrixMessenger2

        matrixMessenger2?.close()
    }
}

suspend fun TestScope.createTestMatrixMultiMessenger(
    coroutineContext: CoroutineContext = Dispatchers.Default
) =
    MatrixMultiMessengerImpl(coroutineContext) {
        messengerConfiguration {
            modulesFactories += createTestDefaultTrixnityMessengerModules().map { { it } }
        }
        modulesFactories = listOf {
            module {
                // TODO there should be a more clean way for I18n
                single<I18n> {
                    object : I18n(
                        DefaultLanguages,
                        createTestMatrixMessengerSettingsHolder(),
                        GetSystemLang { "en" },
                        TimeZone.of("CET"),
                    ) {}
                }
                // TODO this needs to be removed and fixed, as there is no MatrixMessengerSettingsHolderImpl at MultiMessenger level!
                single<MatrixMessengerSettingsHolder> { createTestMatrixMessengerSettingsHolder() }
            }
        } + createTrixnityMultiMessengerDefaultModuleFactories() + {
            module {
                single<MatrixMultiMessengerSettingsHolder> { createTestMatrixMultiMessengerSettingsHolder() }
            }
        }
    }
