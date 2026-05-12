package de.connect2x.trixnity.messenger.compose.view

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import kotlin.coroutines.CoroutineContext

actual suspend fun createTestMatrixMultiMessenger(coroutineContext: CoroutineContext): MatrixMultiMessenger {
    throw Exception("tests requiring a MatrixMultiMessenger should be run as instrumented tests")
}
