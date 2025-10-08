package de.connect2x.trixnity.messenger

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create

class MatrixMultiMessengerService : SingletonService<MatrixMultiMessenger>(
    { applicationContext ->
        MatrixMultiMessenger.create(applicationContext) {
            configuration()
            modulesFactories + ::initialSyncModule
        }
    }
) {
    companion object {
        var configuration: MatrixMultiMessengerConfiguration.() -> Unit = {}
    }
}

fun isMatrixMultiMessengerServiceEnabled(context: Context) =
    context.packageManager.getComponentEnabledSetting(ComponentName(context, MatrixMultiMessengerService::class.java))
        .let {
            it == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                    it == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

suspend fun <T> withMatrixMultiMessengerFromService(
    context: Context,
    block: suspend (matrixMultiMessenger: MatrixMultiMessenger) -> T
): T =
    if (isMatrixMessengerServiceEnabled(context)) withSingletonService(context, block)
    else throw IllegalStateException("MatrixMultiMessengerService is not enabled")
