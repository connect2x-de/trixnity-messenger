package de.connect2x.trixnity.messenger.util

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test

class ListTest {
    @Test
    fun `adding sorted to list - should add element at correct position`() {
        val ls = listOf(1, 2, 4)
        ls.plusSorted(3) shouldBe listOf(1, 2, 3, 4)
    }

    @Test
    fun `adding sorted to list - should add element at correct position and update state`() {
        val ls = MutableStateFlow(listOf(1, 2, 4))
        ls.plusSorted(3)
        ls.value shouldBe listOf(1, 2, 3, 4)
    }
}
