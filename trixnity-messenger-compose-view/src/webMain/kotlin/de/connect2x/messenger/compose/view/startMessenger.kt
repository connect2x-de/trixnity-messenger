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
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.skiko.wasm.onWasmReady

private val log = KotlinLogging.logger {}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
suspend fun startMessenger(
    appName: String,
    version: String,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit,
) {
    log.info { "Starting $appName client (version=${version})" }
    logLevel = Level.DEBUG
    val windowIsFocused = MutableStateFlow(false)
    window.onfocus = {
        windowIsFocused.value = true
        Unit
    }
    window.onblur = {
        windowIsFocused.value = false
        Unit
    }

    val matrixMultiMessenger = MatrixMultiMessenger.create(configuration = configuration)

    log.info { "Created MatrixMultiMessenger" }
    matrixMultiMessenger.singleMode { matrixMessenger ->
        val rootViewModel = matrixMessenger.createRoot()
        val config = matrixMessenger.di.get<MatrixMessengerConfiguration>()
        onWasmReady {
            CanvasBasedWindow(config.appName) {
                CompositionLocalProvider(
                    ImeVisible provides false,
                    Platform provides PlatformType.WEB,
                    IsFocused provides windowIsFocused.collectAsState(false).value,
//                LocalWindowScope provides this@Window, // FIXME
                    IsDebug provides false, // FIXME
                    DI provides matrixMessenger.di,
                ) {
                    MessengerTheme {
                        Client(rootViewModel)
                    }
                }
            }
        }
    }
}
