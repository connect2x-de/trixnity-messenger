/**
 * MIT License
 *
 * Copyright (c) 2018 Wolt Enterprises
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.IntSize
import de.connect2x.trixnity.messenger.compose.view.files.createImageBitmap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.withSign

object BlurHashDecoder {
    /**
     * Decode a blur hash into a new bitmap.
     */
    fun decode(blurHash: String?, intrinsicSize: IntSize?, punch: Float = 1f): ImageBitmap? {
        if (blurHash == null || blurHash.length < 6) {
            return null
        }
        val numCompEnc = Base83.decode(blurHash, 0, 1)
        val numCompX = (numCompEnc % 9) + 1
        val numCompY = (numCompEnc / 9) + 1
        if (blurHash.length != 4 + 2 * numCompX * numCompY) {
            return null
        }
        val maxAcEnc = Base83.decode(blurHash, 1, 2)
        val maxAc = (maxAcEnc + 1) / 166f
        val colors = Array(numCompX * numCompY) { i ->
            if (i == 0) {
                val colorEnc = Base83.decode(blurHash, 2, 6)
                decodeDc(colorEnc)
            } else {
                val from = 4 + i * 2
                val colorEnc = Base83.decode(blurHash, from, from + 2)
                decodeAc(colorEnc, maxAc * punch)
            }
        }
        if (intrinsicSize == null) {
            return null
        }

        return createImageBitmap(intrinsicSize.width, intrinsicSize.height) { x, y ->
            var r = 0f
            var g = 0f
            var b = 0f
            for (j in 0 until numCompY) {
                for (i in 0 until numCompX) {
                    val cosX = cos(PI * x * i / intrinsicSize.width)
                    val cosY = cos(PI * y * j / intrinsicSize.height)
                    val basis = (cosX * cosY).toFloat()
                    val color = colors[j * numCompX + i]
                    r += color[0] * basis
                    g += color[1] * basis
                    b += color[2] * basis
                }
            }
            Color(linearToSrgb(r), linearToSrgb(g), linearToSrgb(b))
        }
    }

    private fun decodeDc(colorEnc: Int): FloatArray {
        val r = colorEnc.shr(16).and(0xFF)
        val g = colorEnc.shr(8).and(0xFF)
        val b = colorEnc.shr(0).and(0xFF)
        return floatArrayOf(srgbToLinear(r), srgbToLinear(g), srgbToLinear(b))
    }

    private fun decodeAc(value: Int, maxAc: Float): FloatArray {
        val r = value / (19 * 19)
        val g = (value / 19) % 19
        val b = value % 19
        return floatArrayOf(
            signedPow2((r - 9) / 9.0f) * maxAc,
            signedPow2((g - 9) / 9.0f) * maxAc,
            signedPow2((b - 9) / 9.0f) * maxAc
        )
    }

    private fun srgbToLinear(colorEnc: Int): Float {
        return ColorSpaces.Srgb.eotf(colorEnc / 255.0).toFloat()
    }

    private fun linearToSrgb(value: Float): Float {
        return ColorSpaces.Srgb.oetf(value.toDouble()).toFloat()
    }

    private fun signedPow2(value: Float) = value.pow(2f).withSign(value)

    private object Base83 {
        fun decode(str: String, from: Int = 0, to: Int = str.length): Int {
            var result = 0
            for (i in from until to) {
                val index = charMap[str[i]] ?: -1
                if (index != -1) {
                    result = result * 83 + index
                }
            }
            return result
        }

        private val charMap = listOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
            'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
            'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '#', '$', '%', '*', '+', ',',
            '-', '.', ':', ';', '=', '?', '@', '[', ']', '^', '_', '{', '|', '}', '~'
        ).mapIndexed { i, c -> c to i }.toMap()
    }
}
