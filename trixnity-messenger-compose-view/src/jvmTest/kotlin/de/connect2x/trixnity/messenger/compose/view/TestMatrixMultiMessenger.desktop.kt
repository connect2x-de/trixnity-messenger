package de.connect2x.trixnity.messenger.compose.view

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.create

actual suspend fun createTestMatrixMultiMessenger(): MatrixMultiMessenger =
    MatrixMultiMessenger.create(configuration = messengerTestConfiguration)
