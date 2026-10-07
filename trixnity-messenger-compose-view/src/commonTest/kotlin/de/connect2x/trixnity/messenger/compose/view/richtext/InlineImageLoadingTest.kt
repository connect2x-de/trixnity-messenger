package de.connect2x.trixnity.messenger.compose.view.richtext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class InlineImageLoadingTest {
    @Test
    fun mediaReadFailuresUseTheTextFallback() = runTest {
        assertNull(
            loadInlineImageOrNull("mxc://example.org/image", 128) { _, _ ->
                throw IllegalStateException("Media read failed")
            }
        )
    }

    @Test
    fun imageLoadingCancellationIsPropagated() = runTest {
        val cancellation = CancellationException("Image is no longer visible")
        assertSame(
            cancellation,
            assertFailsWith<CancellationException> {
                loadInlineImageOrNull("mxc://example.org/image", 128) { _, _ -> throw cancellation }
            },
        )
    }

    @Test
    fun successfulLoadsReturnTheImageBytes() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        assertSame(
            bytes,
            loadInlineImageOrNull("mxc://example.org/image", 128) { uri, size ->
                assertEquals("mxc://example.org/image", uri)
                assertEquals(128, size)
                bytes
            },
        )
    }
}
