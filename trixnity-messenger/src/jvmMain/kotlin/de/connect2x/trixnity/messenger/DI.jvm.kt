package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.platformPathsModule
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun commonPlatformModule(): Module = module {
    includes(platformPathsModule())
}
