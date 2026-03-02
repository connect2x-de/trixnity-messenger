@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.trixnity.messenger.compose.view.files

import js.array.jsArrayOf
import kotlinx.coroutines.test.runTest
import web.blob.Blob
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString
import kotlin.test.Test
import kotlin.test.assertFails

class TestPdfReaderWeb {
    @Test
    fun `invalid pdf`() = runTest {
        pdfjs.GlobalWorkerOptions.workerSrc = "/pdf.worker.mjs"
        assertFails { PdfReaderWeb(Blob(jsArrayOf("not a pdf file".toJsString()))) }
    }
}
