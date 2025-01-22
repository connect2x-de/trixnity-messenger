package de.connect2x.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import org.jetbrains.skiko.wasm.onWasmReady
import web.dom.DocumentVisibilityState
import web.dom.document
import web.events.Event
import web.events.addEventListener
import web.uievents.FocusEvent
import web.window.window

private val log = KotlinLogging.logger {}

@OptIn(ExperimentalComposeUiApi::class)
suspend fun startMessenger(
    configuration: MatrixMultiMessengerConfiguration.() -> Unit,
) {
    log.info { "Starting client" }
    logLevel = Level.DEBUG

    val matrixMultiMessenger = MatrixMultiMessenger.create(configuration = configuration)
    val lifecycleRegistry = LifecycleRegistry(Lifecycle.State.STARTED)
    val windowIsFocused = MutableStateFlow(false)

    log.info { "Created MatrixMultiMessenger" }

    document.addEventListener(
        type = Event.VISIBILITY_CHANGE,
        handler = { _: Event ->
            lifecycleRegistry.updateState(
                document.visibilityState == DocumentVisibilityState.visible,
                windowIsFocused.value
            )
        }
    )

    window.addEventListener(
        type = FocusEvent.FOCUS,
        handler = { _: Event ->
            lifecycleRegistry.updateState(
                visible = document.visibilityState == DocumentVisibilityState.visible,
                focused = windowIsFocused.updateAndGet { true }
            )
        }
    )

    window.addEventListener(
        type = FocusEvent.BLUR,
        handler = { _: Event ->
            lifecycleRegistry.updateState(
                visible = document.visibilityState == DocumentVisibilityState.visible,
                focused = windowIsFocused.updateAndGet { false }
            )
        }
    )

    matrixMultiMessenger.singleMode { matrixMessenger ->
        try {
            val rootViewModel = matrixMessenger.createRoot(DefaultComponentContext(lifecycleRegistry))
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

private fun LifecycleRegistry.updateState(visible: Boolean, focused: Boolean) {
    val target = when {
        visible && focused -> Lifecycle.State.RESUMED
        visible -> Lifecycle.State.STARTED
        else -> Lifecycle.State.CREATED
    }
    if (state != target) {
        log.debug { "Application State changing from $state to $target" }
        while (state < target) when (state) {
            Lifecycle.State.INITIALIZED -> onCreate()
            Lifecycle.State.CREATED -> onStart()
            Lifecycle.State.STARTED -> onResume()
            else -> Unit
        }
        while (state > target) when (state) {
            Lifecycle.State.RESUMED -> onPause()
            Lifecycle.State.STARTED -> onStop()
            Lifecycle.State.CREATED -> onDestroy()
            else -> Unit
        }
    }
}
