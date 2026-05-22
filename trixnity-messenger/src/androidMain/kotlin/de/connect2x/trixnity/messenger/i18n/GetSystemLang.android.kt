package de.connect2x.trixnity.messenger.i18n

import java.util.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformGetSystemLangModule(): Module = module {
    single<GetSystemLang> { GetSystemLang { Locale.getDefault().language } }
}
