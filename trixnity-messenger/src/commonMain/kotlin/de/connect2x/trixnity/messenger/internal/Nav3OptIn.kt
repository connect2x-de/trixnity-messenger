package de.connect2x.trixnity.messenger.internal

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.module

private data object Nav3OptIn

@TrixnityMessengerPrivateApi
val Koin.hasNav3OptIn: Boolean
    get() = getOrNull<Nav3OptIn>() != null

internal fun nav3OptInModuleFactory(): Module {
    return module { single<Nav3OptIn> { Nav3OptIn } }
}
