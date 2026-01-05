package de.connect2x.messenger.compose.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.messenger.compose.view.profiles.IntroductionOrProfile
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.MessengerTheme
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
import java.awt.GraphicsEnvironment
import java.awt.Taskbar
import java.awt.dnd.DropTarget


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
        val escapeKeyPressed = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        LifecycleController(lifecycle, windowState)

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
                        DI provides matrixMessenger.di,
                        IsFocusHighlighting provides isFocusHighlighting,
                        EscapeKeyPressed provides escapeKeyPressed,
                    ) {
                        MessengerTheme {
                            Client(rootViewModel)
                        }
                    }
                },
                nonActiveMessenger = { existingProfiles ->
                    val showProfileCreation = remember { mutableStateOf(false) }
                    CompositionLocalProvider(
                        Platform provides PlatformType.DESKTOP,
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
