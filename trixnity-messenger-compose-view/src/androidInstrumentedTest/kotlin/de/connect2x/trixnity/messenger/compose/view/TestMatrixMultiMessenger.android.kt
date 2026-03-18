package de.connect2x.trixnity.messenger.compose.view

import androidx.test.platform.app.InstrumentationRegistry
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.create
import kotlin.coroutines.CoroutineContext

actual suspend fun createTestMatrixMultiMessenger(coroutineContext: CoroutineContext): MatrixMultiMessenger {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return MatrixMultiMessenger.create(context) {
        messengerTestConfiguration()
    }
}
