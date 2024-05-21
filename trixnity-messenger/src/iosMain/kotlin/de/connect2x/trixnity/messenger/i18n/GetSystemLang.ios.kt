package de.connect2x.trixnity.messenger.i18n

import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun platformGetSystemLangModule(): Module = module {
    single<GetSystemLang> {
        GetSystemLang { NSLocale.currentLocale.languageCode }
    }
}
