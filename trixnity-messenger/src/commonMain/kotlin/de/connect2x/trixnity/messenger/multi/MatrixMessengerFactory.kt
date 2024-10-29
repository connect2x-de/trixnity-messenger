package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerImpl
import de.connect2x.trixnity.messenger.util.RootPath
import org.koin.core.module.Module
import org.koin.dsl.module

fun interface MatrixMessengerFactory {
    suspend operator fun invoke(profileId: String): MatrixMessenger
}

fun matrixMessengerFactoryModule(): Module = module {
    single<MatrixMessengerFactory> {
        val configuration = get<MatrixMultiMessengerConfiguration>().messengerWithBase
        val copyMultiMessengerSingletons = getAll<CopyMultiMessengerSingletons>()
        val rootPath = get<RootPath>().path
        MatrixMessengerFactory { profileId ->
            MatrixMessengerImpl {
                configuration()
                modules += module {
                    single<RootPath> {
                        RootPath(rootPath.resolve(profileId))
                    }
                    copyMultiMessengerSingletons.forEach { it.copy(from = this@single, to = this) }
                }
            }
        }
    }
}
