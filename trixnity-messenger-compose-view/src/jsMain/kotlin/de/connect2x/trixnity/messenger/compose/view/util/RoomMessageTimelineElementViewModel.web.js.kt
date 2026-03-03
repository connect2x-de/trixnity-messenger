@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)

package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipboardItem as ComposeClipboardItem
import web.clipboard.ClipboardItem


internal actual fun newClipEntry(entries: JsArray<ClipboardItem>): ClipEntry =
    ClipEntry(entries.unsafeCast<Array<ComposeClipboardItem>>())

internal actual fun withPlainText(text: String): ClipEntry = ClipEntry.withPlainText(text)

internal actual fun isEmpty(clipEntry: ClipEntry): Boolean = clipEntry.clipboardItems.isEmpty()
