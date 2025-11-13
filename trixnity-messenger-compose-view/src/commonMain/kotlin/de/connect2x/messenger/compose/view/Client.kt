package de.connect2x.messenger.compose.view

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import de.connect2x.messenger.compose.view.root.RootSwitch
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.messenger.compose.view.uia.UiaSwitch
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.Koin


const val SINGLE_PANE_THRESHOLD = 1024
const val TWO_PANE_THRESHOLD = 1100

const val ROOM_LIST_WEIGHT = 0.27f
const val ROOM_WEIGHT = 1f - ROOM_LIST_WEIGHT

enum class PlatformType {
    DESKTOP, WEB, ANDROID, IOS;
}

val PlatformType.isMobile
    get() = this == PlatformType.ANDROID || this == PlatformType.IOS

val PlatformType.isWeb
    get() = this == PlatformType.WEB

val PlatformType.isDesktop
    get() = this == PlatformType.DESKTOP

val Platform = compositionLocalOf<PlatformType> { error("compositionLocal not defined") }
val IsFocused = compositionLocalOf<Boolean> { error("compositionLocal not defined") }
val DI = compositionLocalOf<Koin> { error("DI is not defined as compositionLocal") }

/**
 * Needs to be registered at an element that - at all times - consumes key events. Compose does only get keyboard events
 * on focused elements, so it _has to_ be registered at the window level (Desktop, Web).
 */
val EscapeKeyPressed = compositionLocalOf<Flow<Unit>> { flowOf() }

interface ClientView {
    @Composable
    fun create(rootViewModel: RootViewModel)
}

@Composable
fun Client(rootViewModel: RootViewModel) {
    DI.get<ClientView>().create(rootViewModel)
}

class ClientViewImpl : ClientView {
    @Composable
    override fun create(rootViewModel: RootViewModel) {
        val insets = WindowInsets.safeDrawing
        val headerColor = MaterialTheme.components.systemUi.header
        val footerColor = MaterialTheme.components.systemUi.footer

        ThemedSurface(
            style = MaterialTheme.components.background,
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val top = insets.getTop(this)
                    val bottom = insets.getBottom(this)

                    drawRect(headerColor, topLeft = Offset.Zero, size = size.copy(height = top.toFloat()))
                    drawRect(footerColor, topLeft = Offset(0f, size.height - bottom), size = size.copy(height = bottom.toFloat()))
                }
                .windowInsetsPadding(insets)
        ) {
            RootSwitch(rootViewModel.stack)
            UiaSwitch(rootViewModel.uiaStack)
        }
    }
}

