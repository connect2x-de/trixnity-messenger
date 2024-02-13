package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessengerImpl
import de.connect2x.trixnity.messenger.util.StoragePrefix
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMatrixMessengerFactory(): Module = module {
    single<MatrixMessengerFactory> {
        val configuration = get<MatrixMultiMessengerConfiguration>().messengerWithBase
        val storagePrefix = get<StoragePrefix>().storagePrefix
        MatrixMessengerFactory { profile ->
            MatrixMessengerImpl {
                configuration()
                modules += module {
                    single<StoragePrefix> {
                        StoragePrefix("$storagePrefix$profile/")
                    }
                    copyMultiMessengerSingletons(this@single)
                }
            }
        }
    }
}