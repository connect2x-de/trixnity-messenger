package de.connect2x.trixnity.messenger.i18n

import org.koin.core.module.Module

fun interface GetSystemLang {
    operator fun invoke(): String
}

expect fun platformGetSystemLangModule(): Module
