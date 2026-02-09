package de.connect2x.trixnity.messenger.compose.view

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger

actual suspend fun createTestMatrixMultiMessenger(): MatrixMultiMessenger {
    throw Exception("tests requiring a MatrixMultiMessenger should be run as instrumented tests")
}
