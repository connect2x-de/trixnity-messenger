package de.connect2x.trixnity.messenger.compose.view.richtext

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class InlineImageLoadingTest {
    @Test
    fun mediaReadFailuresUseTheTextFallback() = runTest {
        assertNull(loadInlineImageOrNull { throw IllegalStateException("Media read failed") })
    }

    @Test
    fun imageLoadingCancellationIsPropagated() = runTest {
        val cancellation = CancellationException("Image is no longer visible")
        assertSame(
            cancellation,
            assertFailsWith<CancellationException> { loadInlineImageOrNull { throw cancellation } },
        )
    }

    @Test
    fun successfulLoadsReturnTheImageBytes() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        assertSame(bytes, loadInlineImageOrNull { bytes })
    }
}
