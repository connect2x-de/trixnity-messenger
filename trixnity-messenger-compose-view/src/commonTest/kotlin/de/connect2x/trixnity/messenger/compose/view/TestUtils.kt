package de.connect2x.trixnity.messenger.compose.view

import de.connect2x.lognity.api.backend.Backend
import de.connect2x.lognity.test.TestBackend

fun configureTestLogging() {
    Backend.set(TestBackend)
}
