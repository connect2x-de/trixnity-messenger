package de.connect2x.messenger.compose.view

import androidx.test.platform.app.InstrumentationRegistry
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.create

actual suspend fun createTestMatrixMultiMessenger(): MatrixMultiMessenger {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return MatrixMultiMessenger.create(context) {
        messengerTestConfiguration()
    }
}
