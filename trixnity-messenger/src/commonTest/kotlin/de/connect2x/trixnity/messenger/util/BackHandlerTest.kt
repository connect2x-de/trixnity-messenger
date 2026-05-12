package de.connect2x.trixnity.messenger.util

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class BackHandlerTest {
    @Test
    fun `backStack » should register callbacks at the intended position`() = runTest {
        val cut = BackHandlerImpl()
        val normalPriority = 0
        val higherPriority = 1
        val firstCallback = BackCallback(normalPriority) { }
        val secondCallback = BackCallback(higherPriority) { }
        val thirdCallback = BackCallback(normalPriority) { }

        cut.registerBackCallback(firstCallback)
        cut.stack shouldBeEqual listOf(firstCallback)
        cut.registerBackCallback(secondCallback)
        cut.stack shouldBeEqual listOf(secondCallback, firstCallback)
        cut.registerBackCallback(thirdCallback)
        cut.stack shouldBeEqual listOf(secondCallback, thirdCallback, firstCallback)
    }

    @Test
    fun `goBack » should execute callbacks in accordance with their enabled state`() = runTest {
        val cut = BackHandlerImpl()
        val normalPriority = 0

        val firstCallbackTriggered = MutableStateFlow(0)
        val firstCallbackEnabled = MutableStateFlow(true)
        val firstCallback = BackCallback(normalPriority, firstCallbackEnabled) { firstCallbackTriggered.value++ }

        val secondCallbackTriggered = MutableStateFlow(0)
        val secondCallbackEnabled = MutableStateFlow(true)
        val secondCallback = BackCallback(normalPriority, secondCallbackEnabled) { secondCallbackTriggered.value++ }

        cut.registerBackCallback(firstCallback)
        cut.registerBackCallback(secondCallback)

        cut.stack shouldBeEqual listOf(secondCallback, firstCallback)

        cut.goBack()
        secondCallbackTriggered.value shouldBe 1
        secondCallbackEnabled.value = false
        cut.goBack()
        secondCallbackTriggered.value shouldBe 1
        firstCallbackTriggered.value shouldBe 1
        firstCallbackEnabled.value = false
        cut.goBack()
        secondCallbackTriggered.value shouldBe 1
        firstCallbackTriggered.value shouldBe 1
    }
}
