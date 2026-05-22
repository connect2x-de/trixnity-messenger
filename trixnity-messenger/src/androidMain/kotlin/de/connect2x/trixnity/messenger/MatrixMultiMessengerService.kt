package de.connect2x.trixnity.messenger

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create

class MatrixMultiMessengerService :
    SingletonService<MatrixMultiMessenger>({ applicationContext ->
        MatrixMultiMessenger.create(applicationContext) {
            configuration()
            modulesFactories + ::initialSyncModule
        }
    }) {
    companion object {
        var configuration: MatrixMultiMessengerConfiguration.() -> Unit = {}
    }
}

class MatrixMultiMessengerServiceConnection :
    SingletonServiceConnection<MatrixMultiMessenger, MatrixMultiMessengerService>(
        MatrixMultiMessengerService::class.java
    )

fun isMatrixMultiMessengerServiceEnabled(context: Context) =
    try {
        context.packageManager
            .getServiceInfo(ComponentName(context, MatrixMultiMessengerService::class.java), 0)
            .enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

suspend fun <T> withMatrixMultiMessengerFromService(
    context: Context,
    block: suspend (matrixMultiMessenger: MatrixMultiMessenger) -> T,
): T =
    if (isMatrixMultiMessengerServiceEnabled(context))
        withSingletonService<T, MatrixMultiMessenger, MatrixMultiMessengerService>(context, block)
    else throw IllegalStateException("MatrixMultiMessengerService is not enabled")
