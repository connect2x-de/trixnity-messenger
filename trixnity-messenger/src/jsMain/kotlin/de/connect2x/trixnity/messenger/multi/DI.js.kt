package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.util.platformStoragePrefixModule
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun commonPlatformModule(): Module = module {
    includes(platformStoragePrefixModule())
}