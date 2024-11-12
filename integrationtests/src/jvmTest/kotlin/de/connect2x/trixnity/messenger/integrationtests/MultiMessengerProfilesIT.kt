package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMultiMessenger
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MultiMessengerProfilesIT {

    @Test
    fun shouldHandleMultipleProfiles(): Unit = runTest {
        val multiMessenger = createTestMatrixMultiMessenger(coroutineContext = backgroundScope.coroutineContext)
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

        matrixMessenger1?.close()
        matrixMessenger2?.close()
    }
}
