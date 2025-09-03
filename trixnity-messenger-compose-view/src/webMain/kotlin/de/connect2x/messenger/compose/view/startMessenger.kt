package de.connect2x.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.messenger.compose.view.notifications.Notifications
import de.connect2x.messenger.compose.view.profiles.rememberRootViewModel
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
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
import web.dom.visible
import web.events.Event
import web.events.VISIBILITY_CHANGE
import web.events.addEventListener
import web.focus.BLUR
import web.focus.FOCUS
import web.focus.FocusEvent
import web.prompts.alert
import web.url.URL
import web.window.window

private val log = KotlinLogging.logger {}

private fun getLogLevel(): Level {
    val levelName = URL(window.location.href).searchParams.get("loglevel")
    return Level.entries.find {
        it.name.equals(levelName, ignoreCase = true)
    } ?: Level.INFO
}

@OptIn(ExperimentalComposeUiApi::class)
suspend fun startMessenger(
    configuration: MatrixMultiMessengerConfiguration.() -> Unit,
) {
    log.info { "Starting client" }
    KotlinLoggingConfiguration.logLevel = getLogLevel()

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
                        val isFocusHighlighting =
                            matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                                .collectAsState().value.base.isFocusHighlighting

                        CompositionLocalProvider(
                            DI provides matrixMessenger.di,
                            IsFocusHighlighting provides isFocusHighlighting,
                        ) {
                            if (rootViewModel != null) {
                                MessengerTheme {
                                    Client(rootViewModel)
                                }
                            }
                            Notifications(matrixMessenger, matrixMultiMessenger.activeProfile.value ?: "default") {
                                // TODO: make URI call to open chat
                            }
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
    if (visible || focused) {
        resume()
    } else {
        pause()
    }
}
