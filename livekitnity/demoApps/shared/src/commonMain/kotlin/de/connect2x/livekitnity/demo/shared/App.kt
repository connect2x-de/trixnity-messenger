package de.connect2x.livekitnity.demo.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.connect2x.livekitnity.api.LiveKit
import de.connect2x.livekitnity.compose.local.LiveKitLocal
import de.connect2x.livekitnity.compose.local.VideoTrackRendererLocal
import de.connect2x.livekitnity.compose.types.VideoTrackRenderer
import de.connect2x.livekitnity.demo.shared.permissions.LocalMediaPermissions
import de.connect2x.livekitnity.demo.shared.permissions.MediaPermissions
import de.connect2x.livekitnity.demo.shared.screens.CallScreen
import de.connect2x.livekitnity.demo.shared.screens.ConnectionScreen
import de.connect2x.livekitnity.demo.shared.viewmodels.EntryViewModel

@Composable
public fun App(
    liveKit: LiveKit,
    videoTrackRenderer: VideoTrackRenderer,
    mediaPermissions: MediaPermissions,
) {
  MaterialTheme(
      colorScheme =
          if (isSystemInDarkTheme()) {
            darkColorScheme()
          } else {
            @OptIn(ExperimentalMaterial3ExpressiveApi::class) expressiveLightColorScheme()
          },
  ) {
    CompositionLocalProvider(
        LiveKitLocal provides liveKit,
        VideoTrackRendererLocal provides videoTrackRenderer,
        LocalMediaPermissions provides mediaPermissions,
    ) {
      Scaffold { paddingValues ->
        Box(
            Modifier.padding(paddingValues).consumeWindowInsets(paddingValues).imePadding(),
        ) {
          var connectionInfo by remember { mutableStateOf<EntryViewModel.ConnectionInfo?>(null) }

          when (val currentConnectionInfo = connectionInfo) {
            null -> ConnectionScreen(onConnect = { connectionInfo = it })
            else -> CallScreen(connectionInfo = currentConnectionInfo)
          }
        }
      }
    }
  }
}
