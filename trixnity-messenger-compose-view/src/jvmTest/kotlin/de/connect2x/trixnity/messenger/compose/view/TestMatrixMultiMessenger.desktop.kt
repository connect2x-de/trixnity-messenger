package de.connect2x.trixnity.messenger.compose.view

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.create
import kotlin.coroutines.CoroutineContext

actual suspend fun createTestMatrixMultiMessenger(coroutineContext: CoroutineContext): MatrixMultiMessenger =
    MatrixMultiMessenger.create(configuration = messengerTestConfiguration)
