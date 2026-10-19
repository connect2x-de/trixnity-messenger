package de.connect2x.livekitnity.demo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.connect2x.livekitnity.compose.ui.AndroidVideoTrackRenderer
import de.connect2x.livekitnity.compose.ui.RendererType
import de.connect2x.livekitnity.core.AndroidLiveKit
import de.connect2x.livekitnity.demo.shared.App
import de.connect2x.livekitnity.demo.shared.permissions.AndroidMediaPermissions

public class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val liveKit = AndroidLiveKit(this)
    val videoTrackRenderer = AndroidVideoTrackRenderer(RendererType.Texture)
    val mediaPermissions = AndroidMediaPermissions(this)

    setContent {
      App(
          liveKit = liveKit,
          videoTrackRenderer = videoTrackRenderer,
          mediaPermissions = mediaPermissions,
      )
    }
  }
}
