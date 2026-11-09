package de.connect2x.livekitnity.demo.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import de.connect2x.livekitnity.compose.ui.WebVideoTrackRenderer
import de.connect2x.livekitnity.core.WebLiveKit
import de.connect2x.livekitnity.demo.shared.App
import de.connect2x.livekitnity.demo.shared.permissions.WebMediaPermissions

@OptIn(ExperimentalComposeUiApi::class)
public fun main() {
  val liveKit = WebLiveKit()
  val videoTrackRenderer = WebVideoTrackRenderer()
  val mediaPermissions = WebMediaPermissions()

  ComposeViewport {
    App(
        liveKit = liveKit,
        videoTrackRenderer = videoTrackRenderer,
        mediaPermissions = mediaPermissions,
    )
  }
}
