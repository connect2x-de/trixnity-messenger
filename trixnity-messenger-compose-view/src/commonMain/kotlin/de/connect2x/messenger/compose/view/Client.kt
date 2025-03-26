package de.connect2x.messenger.compose.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.root.RootSwitch
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.messenger.compose.view.uia.UiaSwitch
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import org.koin.core.Koin


const val SINGLE_PANE_THRESHOLD = 1024
const val TWO_PANE_THRESHOLD = 1100

const val ROOM_LIST_WEIGHT = 0.27f
const val ROOM_WEIGHT = 1f - ROOM_LIST_WEIGHT

enum class PlatformType {
    DESKTOP, WEB, ANDROID;
}

val PlatformType.isMobile
    get() = this == PlatformType.ANDROID

val PlatformType.isWeb
    get() = this == PlatformType.WEB

val PlatformType.isDesktop
    get() = this == PlatformType.DESKTOP

val Platform = compositionLocalOf<PlatformType> { error("compositionLocal not defined") }
val IsFocused = compositionLocalOf<Boolean> { error("compositionLocal not defined") }
val DI = compositionLocalOf<Koin> { error("DI is not defined as compositionLocal") }

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
        ThemedSurface(
            style = MaterialTheme.components.background,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            RootSwitch(rootViewModel.stack)
            UiaSwitch(rootViewModel.uiaStack)
        }
    }
}

