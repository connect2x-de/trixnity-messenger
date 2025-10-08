package de.connect2x.trixnity.messenger

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first


class MatrixMessengerService : SingletonService<MatrixMessenger>(
    { applicationContext ->
        MatrixMessenger.create(applicationContext) {
            configuration()
            modulesFactories + ::initialSyncModule
        }
    }
) {
    companion object {
        var configuration: MatrixMessengerConfiguration.() -> Unit = {}
    }
}

fun isMatrixMessengerServiceEnabled(context: Context) =
    context.packageManager.getComponentEnabledSetting(ComponentName(context, MatrixMessengerService::class.java)).let {
        it == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                it == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

suspend fun <T> withMatrixMessengerFromService(
    context: Context,
    block: suspend (matrixMessenger: MatrixMessenger) -> T
): T =
    when {
        isMatrixMessengerServiceEnabled(context) -> withSingletonService(context, block)
        isMatrixMultiMessengerServiceEnabled(context) ->
            withSingletonService<T, MatrixMultiMessenger, MatrixMultiMessengerService>(context) { matrixMultiMessenger ->
                if (matrixMultiMessenger.activeProfile.value == null)
                    throw IllegalArgumentException("no profile active to receive MatrixMessenger")
                block(matrixMultiMessenger.activeMatrixMessenger.filterNotNull().first())
            }

        else -> throw IllegalStateException("MatrixMultiMessengerService is not enabled")
    }
