package de.connect2x.livekitnity.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.track.WebVideoTrack
import de.connect2x.livekitnity.compose.state.rememberTrack
import de.connect2x.livekitnity.compose.types.ScaleType
import de.connect2x.livekitnity.compose.types.TrackReference
import de.connect2x.livekitnity.compose.types.VideoTrackRenderer
import de.connect2x.livekitnity.compose.util.rememberStateOrDefault
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement

public class WebVideoTrackRenderer : VideoTrackRenderer {
  @Composable
  override fun Content(
      trackReference: TrackReference?,
      modifier: Modifier,
      room: Room?,
      mirror: Boolean,
      scaleType: ScaleType,
  ) {
    VideoTrackView(
        trackReference = trackReference,
        modifier = modifier,
        room = room,
        mirror = mirror,
        scaleType = scaleType,
    )
  }
}

@Composable
private fun VideoTrackView(
    trackReference: TrackReference?,
    modifier: Modifier = Modifier,
    room: Room? = null,
    mirror: Boolean = false,
    scaleType: ScaleType = ScaleType.Fill,
) {
  val track by
      rememberStateOrDefault(null) {
        if (trackReference != null) {
          rememberTrack<WebVideoTrack>(trackIdentifier = trackReference)
        } else {
          null
        }
      }

  VideoTrackView(
      videoTrack = track,
      modifier = modifier,
      mirror = mirror,
      scaleType = scaleType,
  )
}

@Composable
private fun VideoTrackView(
    videoTrack: WebVideoTrack?,
    modifier: Modifier = Modifier,
    mirror: Boolean = false,
    scaleType: ScaleType = ScaleType.Fill,
) {
  val video = remember {
    (document.createElement("video") as HTMLVideoElement).apply {
      autoplay = true
      muted = true
      controls = false
      tabIndex = -1
      setAttribute("playsinline", "")
      setAttribute("aria-hidden", "true")
      style.setProperty("display", "block")
      style.setProperty("pointer-events", "none")
      style.setProperty("object-position", "center")
    }
  }

  DisposableEffect(videoTrack, video) {
    videoTrack?.attach(video)
    onDispose { videoTrack?.detach(video) }
  }

  @OptIn(ExperimentalComposeUiApi::class)
  HtmlElementView(
      factory = { video },
      update = {
        it.style.setProperty(
            "object-fit",
            when (scaleType) {
              ScaleType.FitInside -> "contain"
              ScaleType.Fill -> "cover"
            },
        )
        it.style.setProperty("transform", if (mirror) "scaleX(-1)" else "none")
      },
      // Kept separate from track playback because this is a CMP DOM workaround.
      modifier = modifier.htmlUnderlay(video),
  )
}
