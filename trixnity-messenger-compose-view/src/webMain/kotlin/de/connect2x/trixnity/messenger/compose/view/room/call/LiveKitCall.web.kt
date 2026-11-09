package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.livekitnity.api.LiveKit
import de.connect2x.livekitnity.compose.types.VideoTrackRenderer
import de.connect2x.livekitnity.compose.ui.WebVideoTrackRenderer
import de.connect2x.livekitnity.core.WebLiveKit

@Composable
internal actual fun rememberLiveKit(): LiveKit {
    return remember { WebLiveKit() }
}

@Composable
internal actual fun rememberVideoTrackRenderer(): VideoTrackRenderer {
    return remember { WebVideoTrackRenderer() }
}
