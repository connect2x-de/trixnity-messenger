package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.connect2x.livekitnity.api.LiveKit
import de.connect2x.livekitnity.compose.types.VideoTrackRenderer
import de.connect2x.livekitnity.compose.ui.AndroidVideoTrackRenderer
import de.connect2x.livekitnity.compose.ui.RendererType
import de.connect2x.livekitnity.core.AndroidLiveKit

@Composable
internal actual fun rememberLiveKit(): LiveKit {
    val context = LocalContext.current
    return remember(context) { AndroidLiveKit(context) }
}

@Composable
internal actual fun rememberVideoTrackRenderer(): VideoTrackRenderer {
    return remember { AndroidVideoTrackRenderer(RendererType.Texture) }
}
