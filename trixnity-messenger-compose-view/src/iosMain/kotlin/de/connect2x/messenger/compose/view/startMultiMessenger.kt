package de.connect2x.messenger.compose.view

import de.connect2x.trixnity.messenger.MatrixMultiMessengerService
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.uikit.ApplicationDelegate
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCValues
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIApplicationMain

// inspired from https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/mpp/demo/src/uikitMain/kotlin/androidx/compose/mpp/demo/main.uikit.kt
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun startMultiMessenger(
    args: List<String>,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit,
) {
    MatrixMultiMessengerService.configuration = {
        addViewProvider()
        configuration()
    }
    runBlocking {
        MatrixMultiMessengerService.init()
    }
    memScoped {
        val argc = args.size
        val argv = args.map { it.cstr.ptr }.toCValues()
        autoreleasepool {
            UIApplicationMain(argc, argv, null, NSStringFromClass(ApplicationDelegate))
        }
    }
}
