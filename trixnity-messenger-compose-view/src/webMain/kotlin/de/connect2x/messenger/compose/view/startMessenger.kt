package de.connect2x.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.messenger.compose.view.profiles.rememberRootViewModel
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create
import de.connect2x.trixnity.messenger.multi.singleModeMatrixMessenger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import org.jetbrains.skiko.wasm.onWasmReady
import web.dom.DocumentVisibilityState
import web.dom.document
import web.events.Event
import web.events.VISIBILITY_CHANGE
import web.events.addEventListener
import web.prompts.alert
import web.uievents.BLUR
import web.uievents.FOCUS
import web.uievents.FocusEvent
import web.window.window

private val log = KotlinLogging.logger {}

@OptIn(ExperimentalComposeUiApi::class)
suspend fun startMessenger(
    configuration: MatrixMultiMessengerConfiguration.() -> Unit,
) {
    log.info { "Starting client" }
    KotlinLoggingConfiguration.logLevel = Level.DEBUG

    val matrixMultiMessenger = MatrixMultiMessenger.create(configuration = configuration)
    val config = matrixMultiMessenger.di.get<MatrixMessengerBaseConfiguration>()
    val i18n = matrixMultiMessenger.di.get<I18n>()

    if (!isPrimaryInstance(config)) {
        log.info { "${config.appName} is already running, skipping initialization" }
        alert(i18n.alreadyRunningError(config.appName))
        window.location.href = "about:blank" // Redirect to empty tab
        return
    }

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

    coroutineScope {
        val matrixMessengerFlow = matrixMultiMessenger
            .singleModeMatrixMessenger()
            .stateIn(this)

        try {
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
                    ) {
                        val matrixMessenger by matrixMessengerFlow.collectAsState()
                        val rootViewModel = rememberRootViewModel(matrixMessenger, lifecycleRegistry)

                        CompositionLocalProvider(
                            DI provides matrixMessenger.di,
                        ) {
                            if (rootViewModel != null) {
                                MessengerTheme {
                                    Client(rootViewModel)
                                }
                            }
                            Notifications(matrixMessenger)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            // in JS sometimes the original stacktrace gets scrambled by coroutines
            // so this method at least should give a better clue on where to look
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
