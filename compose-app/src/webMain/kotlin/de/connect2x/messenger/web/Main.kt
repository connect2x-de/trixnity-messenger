package de.connect2x.messenger.web

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import de.connect2x.compose_app.generated.resources.Res
import de.connect2x.messenger.BuildConfig
import de.connect2x.messenger.compose.view.Client
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.GetLicences
import de.connect2x.messenger.compose.view.GetLicencesImpl
import de.connect2x.messenger.compose.view.ImeVisible
import de.connect2x.messenger.compose.view.IsDebug
import de.connect2x.messenger.compose.view.IsFocused
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.messengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createRoot
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
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
suspend fun main() {
    log.info { "Starting ${BuildConfig.appName} client (version=${BuildConfig.version})" }
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

    val matrixMultiMessenger = MatrixMultiMessenger.create(configuration = messengerConfiguration())

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
                    GetLicences provides GetLicencesImpl {
                        Res.readBytes("files/aboutlibraries.json").decodeToString()
                    },
                ) {
                    MessengerTheme {
                        Client(rootViewModel)
                    }
                }
            }
        }
    }
}
