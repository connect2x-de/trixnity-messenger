@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalComposeUiApi::class)

package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import web.clipboard.ClipboardItem

internal actual fun newClipEntry(entries: JsArray<ClipboardItem>): ClipEntry = ClipEntry(entries.unsafeCast())

internal actual fun withPlainText(text: String): ClipEntry = ClipEntry.withPlainText(text)

internal actual fun isEmpty(clipEntry: ClipEntry): Boolean = clipEntry.clipboardItems.length == 0
