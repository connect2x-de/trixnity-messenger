package de.connect2x.trixnity.messenger

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@OptIn(BetaInteropApi::class)
object MatrixMessengerService : SingletonService<MatrixMessenger>() {
    override suspend fun factory(): MatrixMessenger = MatrixMessenger.create(configuration = configuration)

    var configuration: MatrixMessengerConfiguration.() -> Unit = {}
}

suspend fun <T> withMatrixMessengerFromService(block: suspend (matrixMessenger: MatrixMessenger) -> T): T {
    val matrixMultiMessenger = MatrixMultiMessengerService.get()
    if (matrixMultiMessenger != null) {
        if (matrixMultiMessenger.activeProfile.value == null)
            throw IllegalArgumentException("no profile active to receive MatrixMessenger")
        return block(matrixMultiMessenger.activeMatrixMessenger.filterNotNull().first())
    } else {
        val matrixMessenger =
            MatrixMessengerService.get() ?: throw IllegalStateException("MatrixMultiMessengerService is not enabled")
        return block(matrixMessenger)
    }
}
