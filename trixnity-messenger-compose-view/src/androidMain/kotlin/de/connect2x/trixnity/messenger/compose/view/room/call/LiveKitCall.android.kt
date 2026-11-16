package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.livekitnity.compose.types.VideoTrackRenderer
import de.connect2x.livekitnity.compose.ui.AndroidVideoTrackRenderer
import de.connect2x.livekitnity.compose.ui.RendererType

@Composable
internal actual fun rememberVideoTrackRenderer(): VideoTrackRenderer {
    return remember { AndroidVideoTrackRenderer(RendererType.Texture) }
}
