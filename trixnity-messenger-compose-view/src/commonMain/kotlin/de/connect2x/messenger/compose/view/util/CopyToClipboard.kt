package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.CopyableContent
import org.koin.core.module.Module

fun interface CopyToClipboard {
    @Composable
    fun create(): suspend (CopyableContent, I18nView) -> Unit
}

expect fun platformCopyToClipboardModule(): Module
