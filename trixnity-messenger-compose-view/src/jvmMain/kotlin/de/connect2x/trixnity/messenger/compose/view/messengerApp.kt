package de.connect2x.trixnity.messenger.compose.view

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
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.compose.view.profiles.Profiles
import de.connect2x.trixnity.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.trixnity.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.trixnity.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.trixnity.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.util.defaultDragAndDropHandler
import de.connect2x.trixnity.messenger.util.defaultUriHandler
import java.awt.GraphicsEnvironment
import java.awt.Taskbar
import java.awt.Toolkit
import java.awt.dnd.DropTarget
import javax.swing.SwingUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okio.FileSystem
import sun.misc.Unsafe

private val logger: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.messengerAppKt")

/**
 * Workaround for https://youtrack.jetbrains.com/issue/CMP-3308 adjusted to use an unsafe callback.
 *
 * Since the module system security makes it impossible to use reflection for this on our minimum target JDK, we need to
 * use sun.misc.Unsafe to try to override the field value, without tripping up reflection security checks. What a
 * beautiful mess.
 */
private fun updateAppClassName(name: String) {
    try {
        val toolkit = Toolkit.getDefaultToolkit()
        if (toolkit::class.java.simpleName != "XToolkit") return
        logger.debug { "AWT Toolkit implementation is ${toolkit::class.java.name}" }
        val classNameField = toolkit.javaClass.getDeclaredField("awtAppClassName")
        logger.debug { "Got field reference $classNameField" }

        try {
            classNameField.isAccessible = true
            classNameField.set(null, name)
        } catch (_: Throwable) {
            val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
            theUnsafe.isAccessible = true
            val unsafe = theUnsafe.get(null) as? Unsafe ?: return
            logger.debug { "Obtained sun.misc.Unsafe instance $unsafe" }

            val fieldBase = unsafe.staticFieldBase(classNameField)
            val fieldOffset = unsafe.staticFieldOffset(classNameField)
            unsafe.putObjectVolatile(fieldBase, fieldOffset, name)
        }

        logger.debug { "Successfully swapped AWT app class to $name" }
    } catch (_: Throwable) {
        // SILENCE
    }
}

fun messengerApp(matrixMultiMessenger: MatrixMultiMessenger, lifecycle: LifecycleRegistry) {
    val appName = matrixMultiMessenger.di.get<MatrixMultiMessengerConfiguration>().appName
    updateAppClassName(appName)

    application(exitProcessOnExit = false) { // Make sure this unblocks and returns after application is closed
        val gd = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
        val width = gd.displayMode.width
        val height = gd.displayMode.height
        val windowState = rememberWindowState(width = min(1600.dp, width.dp), height = min(1000.dp, height.dp))
        val escapeKeyPressed = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        LifecycleController(lifecycle, windowState)

        Window(
            onCloseRequest = ::exitApplication,
            title = appName,
            icon = MessengerTrayIcon(0, iconSize = 1024F),
            state = windowState,
            onPreviewKeyEvent = { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    escapeKeyPressed.tryEmit(Unit)
                    false
                } else false
            },
        ) {
            SwingUtilities.invokeLater { // AWT API should be called from underlying EDT
                if (Taskbar.isTaskbarSupported()) {
                    if (!Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) return@invokeLater
                    Taskbar.getTaskbar().iconImage =
                        MessengerTrayIcon(0, iconSize = 1024F).toAwtImage(Density(1F), LayoutDirection.Ltr)
                }
            }
            WithProfileSelection(
                matrixMultiMessenger = matrixMultiMessenger,
                componentContext = DefaultComponentContext(lifecycle),
                activeMessengerOnce = { matrixMessenger, _ ->
                    window.rootPane.dropTarget = DropTarget()
                    window.rootPane.dropTarget.addDropTargetListener(
                        DragAndDrop(matrixMessenger.defaultDragAndDropHandler, matrixMessenger.di.get<FileSystem>())
                    )
                    // Launch this onto the messenger's coroutine scope so it gets shut down properly
                    matrixMultiMessenger.di.get<CoroutineScope>().launch {
                        matrixMultiMessenger.defaultUriHandler.collect { window.rootPane.requestFocus() }
                    }
                },
                activeMessenger = { matrixMessenger, rootViewModel ->
                    val unreadMessages = matrixMessenger.notificationCount.collectAsState()
                    val trayState = rememberTrayState()
                    if (isTraySupported) { // e.g., for GNOME this is false
                        Tray(state = trayState, icon = MessengerTrayIcon(unreadMessages.value))
                    }
                    val isFocusHighlighting =
                        matrixMessenger.di
                            .get<MatrixMessengerSettingsHolder>()
                            .collectAsState()
                            .value
                            .base
                            .isFocusHighlighting
                    CompositionLocalProvider(
                        Platform provides PlatformType.DESKTOP,
                        DI provides matrixMessenger.di,
                        IsFocusHighlighting provides isFocusHighlighting,
                        EscapeKeyPressed provides escapeKeyPressed,
                    ) {
                        MessengerTheme { Client(rootViewModel) }
                    }
                },
                nonActiveMessenger = {
                    val showProfileCreation = remember { mutableStateOf(false) }
                    CompositionLocalProvider(
                        Platform provides PlatformType.DESKTOP,
                        DI provides matrixMultiMessenger.di,
                        ShowProfileCreation provides showProfileCreation,
                        IsFocusHighlighting provides false,
                        EscapeKeyPressed provides escapeKeyPressed,
                    ) {
                        MessengerTheme { Profiles() }
                    }
                },
            )
        }
    }
}
