package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.IntSize
import de.connect2x.trixnity.messenger.compose.view.util.BlurHashDecoder
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlurHashDecoderTest {

    // Generator: https://onlineminitools.com/blurhash-generator
    // Image: trixnity-messenger-compose-app/src/commonMain/composeResources/drawable/status_icon.png
    // Components X: 4
    // Components Y: 3
    // Max edge (px) for encoding: 16
    private val validFixture =
        Fixture(
            blurHash = "L4BK2q=J1JJR1xS3|]w_1cS3JRE}",
            width = 16,
            height = 16,
            expected =
                "/z0AAP9AAAD/RQAA/0sDAf9RCgX/VA8I/1QSCf9RFAn/SxQI/0USBv8+DwT/OgsB/zoFAP88AAD/QAAA/0MAAP9AAAD/QgAA/0cAAP9NBAL/UgsF/1UQCP9VEwn/UhUK/00VCf9HEwb/QRAE/z0MAv88BQD/PgAA/0EAAP9EAAD/RwAA/0kAAP9NAgD/UQcD/1UNBv9YEgj/VxUK/1UWCv9RFgn/TBUH/0cSBf9DDgP/QggB/0MCAP9FAAD/RwAA/1EBAP9SAwD/VAYC/1cMBP9aEAf/XBQK/1wXC/9aGAv/VxgK/1MXCf9PFQf/TBEF/0oMA/9KBgL/SwEB/0wAAP9aBwL/WwgD/1wMBP9eEAb/XxQJ/2AXC/9gGQz/XxsM/10bDP9aGgv/VxcJ/1UUB/9TEAX/UQoE/1EFA/9RAQL/Yw0F/2MOBf9jEAf/ZBMI/2UXCv9lGgz/ZRwN/2UdDf9kHQ3/YhwM/2AaC/9dFwn/WxMH/1gOBv9WCQX/VQUE/2kQB/9pEQj/aRMJ/2oWCv9qGQz/ahwN/2oeDv9qHw7/ah8O/2keDf9nHAz/ZBkL/2EVCf9eEQj/WwwH/1kIBv9tEwn/bRMJ/20VCv9uGAv/bhsM/24eDf9vIA7/byEP/28hD/9uIA7/bB4N/2obDP9mFwv/YhMJ/14OCP9cCgf/bhMJ/28UCv9vFgr/cBkL/3EcDf9yHw7/cyEO/3QiD/90Ig//ciEO/3AfDv9tHAz/aRgL/2QTCf9gDwj/XQsH/20TCf9tEwn/bxYK/3AZC/9zHAz/dR8N/3YhDv93Ig//dyIP/3UhDv9yHw3/bhwM/2kYC/9kEwn/Xw4I/1wKB/9oEAf/aREI/2wUCf9vGAr/cxwM/3YfDf95IQ7/eSIO/3kiDv92IQ3/ch8M/20bC/9oFwn/YhII/14NB/9aCAb/Yg0F/2MOBf9nEgf/bRYI/3MbCv94Hgz/eiEN/3siDf96Ig3/diAM/3EeC/9rGgn/ZRUH/18PBv9aCQX/VwUE/1kHAv9bCQP/Yg4E/2oUBv9yGQn/eB0L/3wgDP98IQz/eiEM/3UfC/9vHAn/aBgH/2ETBf9bDAT/VgYD/1MBAv9PAQD/UgQA/1sKAv9mEQT/cBcH/3gcCv98Hwv/fSAL/3ogCv90Hgn/bRsH/2QWBf9cEAP/VgkC/1ECAf9PAAD/RQAA/0kAAP9VBgD/Yw8D/28WBv94Gwj/fR4K/30fCv96Hwn/cx0H/2oZBf9hFAP/WA0B/1IFAP9NAAD/SwAA/z0AAP9DAAD/UQMA/2ENAv9uFAX/eBoI/30dCf99Hwr/eR4J/3IcBv9oGAT/XhMC/1UMAP9OAwD/SgAA/0gAAA==",
        )

    private val singlePixelFixture = Fixture(blurHash = "000000", width = 1, height = 1, expected = "/wAAAA==")

    private val landscapeFixture = Fixture(blurHash = "000000", width = 2, height = 1, expected = "/wAAAP8AAAA=")

    private val portraitFixture = Fixture(blurHash = "000000", width = 1, height = 2, expected = "/wAAAP8AAAA=")

    private val wideFixture =
        Fixture(
            blurHash = validFixture.blurHash,
            width = 8,
            height = 4,
            expected =
                "/z0AAP9FAAD/UQoF/1QSCf9LFAj/Pg8E/zoFAP9AAAD/WgcC/1wMBP9fFAn/YBkM/10bDP9XFwn/UxAF/1EFA/9uEwn/bxYK/3EcDf9zIQ7/dCIP/3AfDv9pGAv/YA8I/1kHAv9iDgT/chkJ/3wgDP96IQz/bxwJ/2ETBf9WBgM=",
        )

    private val tallFixture =
        Fixture(
            blurHash = validFixture.blurHash,
            width = 4,
            height = 8,
            expected =
                "/z0AAP9RCgX/SxQI/zoFAP9HAAD/VQ0G/1EWCf9CCAH/WgcC/18UCf9dGwz/UxAF/2kQB/9qGQz/ah8O/2EVCf9uEwn/cRwN/3QiD/9pGAv/aBAH/3McDP95Ig7/aBcJ/1kHAv9yGQn/eiEM/2ETBf9FAAD/bxYG/3ofCf9YDQE=",
        )

    private val smallerThanComponentCountsFixture =
        Fixture(blurHash = validFixture.blurHash, width = 1, height = 1, expected = "/z0AAA==")

    @Test
    fun shouldDecodeAValidBlurHash() {
        testFixture(validFixture)
    }

    @Test
    fun shouldDecodeASinglePixel() {
        testFixture(singlePixelFixture)
    }

    @Test
    fun shouldDecodeALandscapeBitmap() {
        testFixture(landscapeFixture)
    }

    @Test
    fun shouldDecodeAPortraitBitmap() {
        testFixture(portraitFixture)
    }

    @Test
    fun shouldRejectAnEmptyBlurHash() {
        testInvalidFixture("")
    }

    @Test
    fun shouldRejectABlurHashShorterThanTheMinimumLength() {
        testInvalidFixture("00000")
    }

    @Test
    fun shouldRejectATruncatedBlurHash() {
        testInvalidFixture(validFixture.blurHash.dropLast(1))
    }

    @Test
    fun shouldRejectABlurHashWithExtraCharacters() {
        testInvalidFixture(validFixture.blurHash + "0")
    }

    @Test
    fun shouldRejectAnInvalidBase83Character() {
        testInvalidFixture("00000!")
    }

    @Test
    fun shouldDecodeToAWiderAspectRatioThanTheComponentGrid() {
        testFixture(wideFixture)
    }

    @Test
    fun shouldDecodeToATallerAspectRatioThanTheComponentGrid() {
        testFixture(tallFixture)
    }

    @Test
    fun shouldDecodeToDimensionsSmallerThanTheComponentCounts() {
        testFixture(smallerThanComponentCountsFixture)
    }

    private fun testFixture(fixture: Fixture) {
        val bitmap = assertNotNull(BlurHashDecoder.decode(fixture.blurHash, IntSize(fixture.width, fixture.height)))

        assertEquals(fixture.width, bitmap.width)
        assertEquals(fixture.height, bitmap.height)
        assertEquals(ColorSpaces.Srgb, bitmap.colorSpace)
        assertEquals(ImageBitmapConfig.Argb8888, bitmap.config)
        assertTrue(bitmap.hasAlpha)

        val pixels = IntArray(fixture.width * fixture.height)
        bitmap.readPixels(
            buffer = pixels,
            startX = 0,
            startY = 0,
            width = fixture.width,
            height = fixture.height,
            bufferOffset = 0,
            stride = fixture.width,
        )

        assertTrue(pixels.all { (it ushr 24) == 0xFF }, "All decoded pixels should be opaque")

        val actual = pixels.toBase64()
        assertEquals(
            expected = fixture.expected,
            actual = actual,
            "Pixel mismatch. Copy this into your browser to see the difference:\n${failurePreviewUrl(fixture, actual)}",
        )
    }

    private fun testInvalidFixture(blurHash: String) {
        assertNull(BlurHashDecoder.decode(blurHash, IntSize(64, 64)), "Expected decoding to fail for: '$blurHash'")
    }

    private fun IntArray.toBase64(): String {
        val bytes = ByteArray(size * 4) { index -> (this[index / 4] ushr (24 - (index % 4) * 8)).toByte() }
        return Base64.encode(bytes)
    }

    private class Fixture(val blurHash: String, val width: Int, val height: Int, val expected: String)

    private fun failurePreviewUrl(fixture: Fixture, actual: String): String {
        val html =
            """
        <!doctype html>
        <html>
        <meta charset="utf-8">
        <title>BlurHash test failure</title>
        <style>
            body { font-family: sans-serif; background: #ddd; }
            main { display: flex; gap: 24px; }
            canvas { width: 320px; height: auto; }
        </style>
        <h1>BlurHash test failure</h1>
        <main>
            <section>
                <h2>Expected</h2>
                <canvas id="expected"></canvas>
            </section>
            <section>
                <h2>Actual</h2>
                <canvas id="actual"></canvas>
            </section>
        </main>
        <script>
            function draw(id, base64) {
                const argb = Uint8Array.from(
                    atob(base64), c => c.charCodeAt(0)
                );
                const rgba = new Uint8ClampedArray(argb.length);
                for (let i = 0; i < argb.length; i += 4) {
                    rgba[i]     = argb[i + 1];
                    rgba[i + 1] = argb[i + 2];
                    rgba[i + 2] = argb[i + 3];
                    rgba[i + 3] = argb[i];
                }
                const canvas = document.getElementById(id);
                canvas.width = ${fixture.width};
                canvas.height = ${fixture.height};
                canvas.getContext("2d").putImageData(
                    new ImageData(rgba, canvas.width, canvas.height),
                    0, 0
                );
            }
            draw("expected", "${fixture.expected}");
            draw("actual", "$actual");
        </script>
        </html>
    """
                .trimIndent()

        return "data:text/html;base64," + Base64.encode(html.encodeToByteArray())
    }
}
