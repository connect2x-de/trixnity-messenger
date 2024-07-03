package de.connect2x.trixnity.messenger.viewmodel.util

import dev.mokkery.matcher.*

import dev.mokkery.answering.*

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import kotlinx.coroutines.cancelChildren

fun TestScope.cancelNeverEndingCoroutines() = this.coroutineContext.cancelChildren()

fun ShouldSpec.withCoroutinesShould(name: String, test: suspend TestScope.() -> Unit) {
    should(name) {
        test(this)
        cancelNeverEndingCoroutines()
    }
}
