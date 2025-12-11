package de.connect2x.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Connect2xComposeUiApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.messenger.compose.view.notifications.Notifications
import de.connect2x.messenger.compose.view.profiles.Profiles
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import web.dom.DocumentVisibilityState
import web.dom.document
import web.dom.visible
import web.events.Event
import web.events.EventType
import web.events.VISIBILITY_CHANGE
import web.events.addEventListener
import web.focus.BLUR
import web.focus.FOCUS
import web.focus.FocusEvent
import web.keyboard.KeyboardEvent
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

@OptIn(ExperimentalComposeUiApi::class, Connect2xComposeUiApi::class, InternalComposeUiApi::class)
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
    val escapeKeyPressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

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

    window.addEventListener(
        type = EventType("keydown"),
        handler = { event: Event ->
            (event as? KeyboardEvent)?.let { keyboardEvent ->
                if (keyboardEvent.key == "Escape") {
                    escapeKeyPressed.tryEmit(Unit)
                }
            }
        })

    coroutineScope {
        try {
            AccessibleComposeViewport {
                // As this is hopefully only temporary until FontFallback works automatically on Web with
                // Browser installed fonts, this is just put here instead of complicating the Theme definition
                // commonMain/kotlin/de/connect2x/messenger/compose/view/theme/Theme.kt with platform specific
                // implementations.
                // When this is removed we can also stop shipping the 6MB of NotoColoEmoji.ttf on Web.
                PreloadEmojis()

                WithProfileSelection(
                    matrixMultiMessenger = matrixMultiMessenger,
                    componentContext = DefaultComponentContext(lifecycleRegistry),
                    activeMessengerOnce = { _, _ -> },
                    activeMessenger = { matrixMessenger, rootViewModel ->
                        val isFocused = windowIsFocused.collectAsState(false).value
                        val isFocusHighlighting =
                            matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                                .collectAsState().value.base.isFocusHighlighting

                        CompositionLocalProvider(
                            Platform provides PlatformType.WEB,
                            IsFocused provides isFocused,
                            DI provides matrixMessenger.di,
                            IsFocusHighlighting provides isFocusHighlighting,
                            EscapeKeyPressed provides escapeKeyPressed,
                        ) {
                            MessengerTheme {
                                Client(rootViewModel)
                            }
                            Notifications(matrixMessenger, matrixMultiMessenger.activeProfile.value ?: "default") {
                                // TODO: make URI call to open chat
                            }
                        }
                    },
                    nonActiveMessenger = { existingProfiles ->
                        val isFocused = windowIsFocused.collectAsState(false).value
                        val showProfileCreation = remember { mutableStateOf(false) }

                        CompositionLocalProvider(
                            Platform provides PlatformType.WEB,
                            IsFocused provides isFocused,
                            DI provides matrixMultiMessenger.di,
                            ShowProfileCreation provides showProfileCreation,
                            IsFocusHighlighting provides false,
                            EscapeKeyPressed provides escapeKeyPressed,
                        ) {
                            MessengerTheme {
                                Profiles()
                            }
                        }
                    }
                )
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
