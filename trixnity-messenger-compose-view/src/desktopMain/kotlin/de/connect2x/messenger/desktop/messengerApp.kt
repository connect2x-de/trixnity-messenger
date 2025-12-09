package de.connect2x.messenger.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.messenger.compose.view.Client
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.EscapeKeyPressed
import de.connect2x.messenger.compose.view.IsFocused
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.compose.view.notifications.Notifications
import de.connect2x.messenger.compose.view.profiles.IntroductionOrProfile
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.sysnotify.withActivationHandler
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.util.UrlHandler
import de.connect2x.trixnity.messenger.util.defaultDragAndDropHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okio.FileSystem
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Taskbar
import java.awt.dnd.DropTarget
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent


private val log = KotlinLogging.logger {}

fun CoroutineScope.messengerApp(
    matrixMultiMessenger: MatrixMultiMessenger,
    lifecycle: LifecycleRegistry,
    urlHandler: UrlHandler
) {
    application {
        val gd = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
        val width = gd.displayMode.width
        val height = gd.displayMode.height
        val windowState = rememberWindowState(
            width = min(1600.dp, width.dp),
            height = min(1000.dp, height.dp)
        )
        var windowIsFocused by remember { mutableStateOf(false) }
        val escapeKeyPressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        Window(
            onCloseRequest = ::exitApplication,
            icon = MessengerTrayIcon(0),
            title = matrixMultiMessenger.di.get<MatrixMultiMessengerConfiguration>().appName,
            state = windowState,
            onPreviewKeyEvent = { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    escapeKeyPressed.tryEmit(Unit)
                    false
                } else false
            }
        ) {
            WithProfileSelection(
                matrixMultiMessenger = matrixMultiMessenger,
                componentContext = DefaultComponentContext(lifecycle),
                activeMessengerOnce = { matrixMessenger, _ ->
                    window.addWindowFocusListener(object : WindowAdapter() {
                        override fun windowGainedFocus(e: WindowEvent?) {
                            log.debug { "window is focused" }
                            windowIsFocused = true
                            lifecycle.resume()
                        }

                        override fun windowLostFocus(e: WindowEvent?) {
                            log.debug { "window has lost focus" }
                            windowIsFocused = false
                            lifecycle.pause()
                        }
                    })


                    window.rootPane.dropTarget = DropTarget()
                    window.rootPane.dropTarget.addDropTargetListener(
                        DragAndDrop(
                            matrixMessenger.defaultDragAndDropHandler,
                            matrixMessenger.di.get<FileSystem>()
                        )
                    )

                    launch {
                        urlHandler.collect {
                            window.rootPane.requestFocus()
                        }
                    }
                },
                activeMessenger = { matrixMessenger, rootViewModel ->
                    val unreadMessages = matrixMessenger.notificationCount.collectAsState()
                    val trayState = rememberTrayState()
                    if (isTraySupported) { // e.g., for GNOME this is false
                        Tray(
                            state = trayState,
                            icon = MessengerTrayIcon(unreadMessages.value),
                        )
                    }
                    if (Taskbar.isTaskbarSupported()) {
                        if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
                            Taskbar.getTaskbar().iconImage =
                                MessengerTrayIcon(unreadMessages.value, iconSize = 1024f).toAwtImage(
                                    Density(1f),
                                    LayoutDirection.Ltr
                                )
                        }
                    }

                    val isFocusHighlighting =
                        matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                            .collectAsState().value.base.isFocusHighlighting
                    CompositionLocalProvider(
                        Platform provides PlatformType.DESKTOP,
                        IsFocused provides windowIsFocused,
                        DI provides matrixMessenger.di,
                        IsFocusHighlighting provides isFocusHighlighting,
                        EscapeKeyPressed provides escapeKeyPressed,
                    ) {
                        MessengerTheme {
                            Client(rootViewModel)
                        }

                        Notifications(matrixMessenger, matrixMultiMessenger.activeProfile.value ?: "default") {
                            withActivationHandler { notification ->
                                // First bring up the window manually since desktop doesn't handle this consistently
                                window.state = Frame.NORMAL
                                window.requestFocus()
                            }
                        }
                    }
                },
                nonActiveMessenger = { existingProfiles ->
                    val showProfileCreation = remember { mutableStateOf(false) }
                    CompositionLocalProvider(
                        Platform provides PlatformType.DESKTOP,
                        IsFocused provides windowIsFocused,
                        DI provides matrixMultiMessenger.di,
                        ShowProfileCreation provides showProfileCreation,
                        IsFocusHighlighting provides false,
                        EscapeKeyPressed provides escapeKeyPressed,
                    ) {
                        MessengerTheme {
                            IntroductionOrProfile()
                        }
                    }
                }
            )
        }
    }
}
