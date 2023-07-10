package de.connect2x.trixnity.messenger.viewmodel.util

import io.kotest.core.test.TestScope
import kotlinx.coroutines.cancelChildren

fun TestScope.cancelNeverEndingCoroutines() = this.coroutineContext.cancelChildren()

