package de.connect2x.trixnity.messenger.multi

import org.koin.core.module.Module
import org.koin.dsl.module

fun createDefaultTrixnityMultiMessengerModules() = listOf(
    module {
        single<ProfileManager> {
            ProfileManagerImpl(get(), get(), get())
        }
    },
    commonPlatformModule(),
    platformMatrixMultiMessengerSettingsHolderModule(),
    platformMatrixMessengerFactory(),
)

expect fun commonPlatformModule(): Module