package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.CopyableContent
import org.koin.core.module.Module

fun interface ToClipboardEntry {
    @Composable
    fun invoke(): suspend (CopyableContent, I18nView) -> ClipEntry
}

expect fun platformCopyToClipboardModule(): Module
