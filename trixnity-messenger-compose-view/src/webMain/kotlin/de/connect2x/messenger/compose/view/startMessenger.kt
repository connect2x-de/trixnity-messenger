package de.connect2x.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createRoot
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create
import de.connect2x.trixnity.messenger.multi.singleMode
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration.logLevel
import io.github.oshai.kotlinlogging.Level
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skiko.wasm.onWasmReady

private val log = KotlinLogging.logger {}

@OptIn(ExperimentalComposeUiApi::class)
suspend fun startMessenger(
    configuration: MatrixMultiMessengerConfiguration.() -> Unit,
) {
    log.info { "Starting client" }
    logLevel = Level.DEBUG
    val windowIsFocused = MutableStateFlow(false)
    window.onfocus = {
        log.debug { "window is focused" }
        windowIsFocused.value = true
        Unit
    }
    window.onblur = {
        log.debug { "window is blurred" }
        windowIsFocused.value = false
        Unit
    }

    val matrixMultiMessenger = MatrixMultiMessenger.create(configuration = configuration)

    log.info { "Created MatrixMultiMessenger" }
    matrixMultiMessenger.singleMode { matrixMessenger ->
        try {
            val rootViewModel = matrixMessenger.createRoot()
            val config = matrixMessenger.di.get<MatrixMessengerConfiguration>()
            onWasmReady {
                CanvasBasedWindow(config.appName) {
                    // As this is hopefully only temporary until FontFallback works automatically on Web with
                    // Browser installed fonts, this is just put here instead of complicating the Theme definition
                    // commonMain/kotlin/de/connect2x/messenger/compose/view/theme/Theme.kt with platform specific
                    // implementations.
                    // When this is removed we can also stop shipping the 6MB of NotoColoEmoji.ttf on Web.
                    PreloadEmojis()
                    CompositionLocalProvider(
                        Platform provides PlatformType.WEB,
                        IsFocused provides windowIsFocused.collectAsState(false).value,
                        IsDebug provides false, // FIXME
                        DI provides matrixMessenger.di,
                    ) {
                        MessengerTheme {
                            Client(rootViewModel)
                        }
                        Notifications(
                            matrixMessenger,
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            // in JS sometimes the original stacktrace gets scrambled by coroutines, so this method at least should give a better clou on where to look
            println(e.message)
            println("-------")
            println(e.stackTraceToString())
            throw e
        }
    }
}
