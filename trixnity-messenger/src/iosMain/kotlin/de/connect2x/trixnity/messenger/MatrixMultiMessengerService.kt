package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create
import org.koin.core.Koin

object MatrixMultiMessengerService : SingletonService<MatrixMultiMessenger>() {
    override suspend fun factory(): MatrixMultiMessenger =
        MatrixMultiMessenger.create(configuration = configuration)

    var configuration: MatrixMultiMessengerConfiguration.() -> Unit = {}
}

suspend fun <T> withMatrixMultiMessengerFromService(
    block: suspend (matrixMultiMessenger: MatrixMultiMessenger) -> T
): T {
    val matrixMultiMessenger = MatrixMultiMessengerService.get()
        ?: throw IllegalStateException("MatrixMultiMessengerService is not enabled")
    return block(matrixMultiMessenger)
}

val activeDI: Koin
    get() = checkNotNull(MatrixMultiMessengerService.get()?.di ?: MatrixMessengerService.get()?.di) {
        "No Active Matrix Messenger, you have to either initialize MatrixMultiMessengerService or MatrixMessengerService"
    }
