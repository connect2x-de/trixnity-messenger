package de.connect2x.trixnity.messenger.i18n

import org.koin.core.module.Module
import org.koin.dsl.module
import web.navigator.navigator

actual fun platformGetSystemLangModule(): Module = module {
    single<GetSystemLang> {
        GetSystemLang { navigator.language }
    }
}
