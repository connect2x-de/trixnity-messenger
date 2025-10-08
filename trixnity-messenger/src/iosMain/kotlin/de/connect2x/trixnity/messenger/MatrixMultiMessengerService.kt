package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIWindowSceneDelegateProtocol

object MatrixMultiMessengerService : SingletonService<MatrixMultiMessenger>() {
    override suspend fun factory(): MatrixMultiMessenger =
        MatrixMultiMessenger.create(configuration = configuration)

    var configuration: MatrixMultiMessengerConfiguration.() -> Unit = {}

    abstract class UIApplicationDelegate :
        UIApplicationDelegateProxy(get()?.di?.getAll<UIApplicationDelegateProtocol>() ?: emptyList())

    abstract class UIWindowSceneDelegate :
        UIWindowSceneDelegateProxy(get()?.di?.getAll<UIWindowSceneDelegateProtocol>() ?: emptyList())
}

suspend fun <T> withMatrixMultiMessengerFromService(
    block: suspend (matrixMultiMessenger: MatrixMultiMessenger) -> T
): T {
    val matrixMultiMessenger = MatrixMultiMessengerService.get()
        ?: throw IllegalStateException("MatrixMultiMessengerService is not enabled")
    return block(matrixMultiMessenger)
}
