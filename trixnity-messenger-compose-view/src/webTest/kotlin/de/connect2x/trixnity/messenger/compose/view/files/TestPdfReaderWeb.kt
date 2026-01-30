package de.connect2x.trixnity.messenger.compose.view.files

import kotlinx.coroutines.test.runTest
import web.blob.Blob
import kotlin.test.Test
import kotlin.test.assertFails

class TestPdfReaderWeb {
    @Test
    fun `invalid pdf`() = runTest {
        pdfjs.GlobalWorkerOptions.workerSrc = "/pdf.worker.mjs"
        assertFails { PdfReaderWeb(Blob(arrayOf("not a pdf file"))) }
    }
}
