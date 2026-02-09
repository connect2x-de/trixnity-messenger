package de.connect2x.trixnity.messenger

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.koin.core.Koin


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

class MatrixMessengerServiceConnection :
    SingletonServiceConnection<MatrixMessenger, MatrixMessengerService>(MatrixMessengerService::class.java)

fun isMatrixMessengerServiceEnabled(context: Context) =
    try {
        context.packageManager.getServiceInfo(ComponentName(context, MatrixMessengerService::class.java), 0)
            .enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

suspend fun <T> withMatrixMessengerFromService(
    context: Context,
    block: suspend (matrixMessenger: MatrixMessenger) -> T,
): T =
    when {
        isMatrixMessengerServiceEnabled(context) -> {
            withSingletonService<T, MatrixMessenger, MatrixMessengerService>(context, block)
        }

        isMatrixMultiMessengerServiceEnabled(context) ->
            withSingletonService<T, MatrixMultiMessenger, MatrixMultiMessengerService>(context) { matrixMultiMessenger ->
                if (matrixMultiMessenger.activeProfile.value == null)
                    throw IllegalArgumentException("no profile active to receive MatrixMessenger")
                block(matrixMultiMessenger.activeMatrixMessenger.filterNotNull().first())
            }

        else -> throw IllegalStateException("MatrixMultiMessengerService is not enabled")
    }

suspend fun <T> withDiFromService(
    context: Context,
    block: suspend (di: Koin) -> T,
): T =
    when {
        isMatrixMessengerServiceEnabled(context) -> {
            withSingletonService<T, MatrixMessenger, MatrixMessengerService>(context) {
                block(it.di)
            }
        }

        isMatrixMultiMessengerServiceEnabled(context) ->
            withSingletonService<T, MatrixMultiMessenger, MatrixMultiMessengerService>(context) {
                block(it.di)
            }

        else -> throw IllegalStateException("MatrixMultiMessengerService is not enabled")
    }
