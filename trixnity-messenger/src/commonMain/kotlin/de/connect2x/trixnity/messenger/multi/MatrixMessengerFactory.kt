package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerImpl
import de.connect2x.trixnity.messenger.util.RootPath
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

fun interface MatrixMessengerFactory {
    suspend operator fun invoke(profileId: String): MatrixMessenger
}

class MatrixMessengerFactoryImpl(
    private val di: Scope,
    private val messengerConfiguration: MatrixMultiMessengerConfiguration,
    private val copyMultiMessengerSingletons: List<CopyMultiMessengerSingletons>,
    private val coroutineScope: CoroutineScope,
    private val rootPath: RootPath,
) : MatrixMessengerFactory {
    override suspend fun invoke(profileId: String): MatrixMessenger =
        MatrixMessengerImpl(coroutineScope.coroutineContext) {
            messengerConfiguration.messengerWithBase(this@MatrixMessengerImpl)
            modulesFactories += {
                module {
                    single<RootPath> { RootPath(rootPath.path.resolve(profileId)) }
                    copyMultiMessengerSingletons.forEach { it.copy(from = di, to = this) }
                }
            }
        }
}

fun matrixMessengerFactoryModule(): Module = module {
    single<MatrixMessengerFactory> { MatrixMessengerFactoryImpl(this, get(), getAll(), get(), get()) }
}
