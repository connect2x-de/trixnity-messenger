package de.connect2x.trixnity.messenger.notification

import de.connect2x.sysnotify.NotificationIcon
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.CoreGraphics.CGSize
import platform.Foundation.NSBundle
import platform.UIKit.UIImage
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun getPlatformNotificationIconModule(): Module = module {
    single<GetNotificationIcon> {
        object : AbstractGetNotificationIcon() {
            override fun fromResource(path: String): NotificationIcon? = memScoped {
                val bundle = NSBundle.mainBundle
                check('/' !in path && '\\' !in path) { "App icon name may not contain path elements" }
                val fileName = path.substringBeforeLast('.')
                val fileExtension = path.substringAfterLast('.')
                val path = bundle.pathForResource(fileName, fileExtension) ?: return null
                val image = UIImage.imageWithContentsOfFile(path) ?: return null
                val data = image.toByteArray() ?: return null
                val size = alloc<CGSize>()
                memcpy(size.ptr, image.size, sizeOf<CGSize>().convert())
                val width = (size.width * image.scale).toInt()
                val height = (size.height * image.scale).toInt()
                return fromBytes(data, width, height)
            }
        }
    }
}
