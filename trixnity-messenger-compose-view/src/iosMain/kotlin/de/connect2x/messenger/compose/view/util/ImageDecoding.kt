package de.connect2x.messenger.compose.view.util

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun decodeImageRGBA8888(
    imageData: ByteArray,
    newWidth: Int,
    newHeight: Int
): ByteArray? {
    return imageData.usePinned { pinnedInputData ->
        val inputAddress = pinnedInputData.addressOf(0)
        val image = UIImage.imageWithData(NSData.create(inputAddress.reinterpret(), imageData.size.convert()))
            ?: return null
        val imageRef = image.CGImage
        val actualWidth = if (newWidth == -1) CGImageGetWidth(imageRef).convert<Int>() else newWidth
        val actualHeight = if (newHeight == -1) CGImageGetHeight(imageRef).convert<Int>() else newHeight
        ByteArray((actualWidth * actualHeight) shl 2).apply {
            usePinned { pinnedOutputData ->
                val outputAddress = pinnedOutputData.addressOf(0)
                val colorSpace = CGColorSpaceCreateDeviceRGB() ?: return@usePinned
                val context = CGBitmapContextCreate(
                    data = outputAddress,
                    width = actualWidth.convert(),
                    height = actualHeight.convert(),
                    bitsPerComponent = 8U,
                    bytesPerRow = actualWidth.toULong() shl 2,
                    space = colorSpace,
                    bitmapInfo = kCGBitmapByteOrder32Big
                )
                if (context == null) {
                    CGColorSpaceRelease(colorSpace)
                    return@usePinned
                }
                val imageRect = CGRectMake(0.0, 0.0, actualWidth.toDouble(), actualHeight.toDouble())
                CGContextDrawImage(context, imageRect, imageRef)
                CGContextRelease(context)
                CGColorSpaceRelease(colorSpace)
            }
        }
    }
}
