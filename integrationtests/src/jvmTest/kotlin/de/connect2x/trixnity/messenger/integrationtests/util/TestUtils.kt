package de.connect2x.trixnity.messenger.integrationtests.util

import de.connect2x.lognity.api.backend.Backend
import de.connect2x.lognity.api.logger.Level
import de.connect2x.lognity.test.TestBackend

fun configureTestLogging() {
    Backend.set(TestBackend)
    val defaultConfig = TestBackend.configSpec
    TestBackend.configSpec = {
        defaultConfig()
        level = Level.INFO // DockerJava produces too much noise
    }
}
