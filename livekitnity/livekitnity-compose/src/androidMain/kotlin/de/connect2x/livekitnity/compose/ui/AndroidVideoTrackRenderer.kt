package de.connect2x.livekitnity.compose.ui

import android.view.Gravity
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import de.connect2x.livekitnity.api.room.AndroidRoom
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.track.AndroidRemoteVideoTrack
import de.connect2x.livekitnity.api.room.track.AndroidVideoTrack
import de.connect2x.livekitnity.compose.local.requireRoom
import de.connect2x.livekitnity.compose.state.rememberTrack
import de.connect2x.livekitnity.compose.types.ScaleType
import de.connect2x.livekitnity.compose.types.TrackReference
import de.connect2x.livekitnity.compose.types.VideoTrackRenderer
import de.connect2x.livekitnity.compose.util.rememberStateOrDefault
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.renderer.TextureViewRenderer
import livekit.org.webrtc.RendererCommon
import livekit.org.webrtc.RendererCommon.RendererEvents
import livekit.org.webrtc.VideoSink

public class AndroidVideoTrackRenderer(
    private val rendererType: RendererType,
) : VideoTrackRenderer {
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
        rendererType = rendererType,
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
    rendererType: RendererType = RendererType.Texture,
    onFirstFrameRendered: () -> Unit = {},
) {
  val track by
      rememberStateOrDefault(null) {
        if (trackReference != null) {
          rememberTrack<AndroidVideoTrack>(trackIdentifier = trackReference)
        } else {
          null
        }
      }

  VideoTrackView(
      videoTrack = track,
      modifier = modifier,
      passedRoom = room,
      mirror = mirror,
      scaleType = scaleType,
      rendererType = rendererType,
      onFirstFrameRendered = onFirstFrameRendered,
  )
}

@Composable
private fun VideoTrackView(
    videoTrack: AndroidVideoTrack?,
    modifier: Modifier = Modifier,
    passedRoom: Room? = null,
    mirror: Boolean = false,
    scaleType: ScaleType = ScaleType.Fill,
    rendererType: RendererType = RendererType.Texture,
    onFirstFrameRendered: (() -> Unit)? = null,
) {
  // Show a black box for preview.
  if (LocalView.current.isInEditMode) {
    Box(
        modifier = Modifier.background(Color.Black).then(modifier),
    )
    return
  }

  val room = requireRoom(passedRoom) as AndroidRoom

  val videoSinkVisibility = remember(room, videoTrack) { ComposeVisibility() }
  var boundVideoTrack by remember { mutableStateOf<AndroidVideoTrack?>(null) }
  var rendererCompat: RendererCompat? by remember { mutableStateOf(null) }
  val firstFrameRenderCallbackState = remember {
    mutableStateOf(onFirstFrameRendered)
  }
      .apply { value = onFirstFrameRendered }

  var rendererEvents by remember {
    mutableStateOf(
        object : RendererEvents {
          override fun onFirstFrameRendered() {
            firstFrameRenderCallbackState.value?.invoke()
          }

          override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
            // Intentionally left blank.
          }
        },
    )
  }

  fun cleanupVideoTrack() {
    rendererCompat?.let { boundVideoTrack?.removeRenderer(it.sink) }
    boundVideoTrack = null
  }

  fun setupVideoIfNeeded(videoTrack: AndroidVideoTrack?, view: VideoSink) {
    if (boundVideoTrack == videoTrack) {
      return
    }

    cleanupVideoTrack()

    boundVideoTrack = videoTrack
    if (videoTrack != null) {
      if (videoTrack is AndroidRemoteVideoTrack) {
        videoTrack.addRenderer(view, videoSinkVisibility)
      } else {
        videoTrack.addRenderer(view)
      }
    }
  }

  DisposableEffect(rendererCompat, mirror) {
    rendererCompat?.setMirror(mirror)
    onDispose {}
  }

  DisposableEffect(room, videoTrack) {
    onDispose {
      videoSinkVisibility.onDispose()
      cleanupVideoTrack()
    }
  }

  DisposableEffect(currentCompositeKeyHashCode.toString()) {
    onDispose {
      rendererCompat?.release()
    }
  }

  fun updateView(v: View) {
    val compat = v.tag as RendererCompat
    setupVideoIfNeeded(videoTrack, compat.sink)

    when (scaleType) {
      ScaleType.FitInside -> {
        compat.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
      }

      ScaleType.Fill -> {
        compat.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
      }
    }
  }

  when (rendererType) {
    RendererType.Surface -> {
      AndroidView(
          factory = { context ->
            FrameLayout(context).apply {
              val renderer =
                  SurfaceViewRenderer(context).apply {
                    init(room.eglBaseContext, rendererEvents)
                    setupVideoIfNeeded(videoTrack, this)
                  }
              addView(
                  renderer,
                  FrameLayout.LayoutParams(
                      FrameLayout.LayoutParams.WRAP_CONTENT,
                      FrameLayout.LayoutParams.WRAP_CONTENT,
                      Gravity.CENTER,
                  ),
              )

              val compat = SurfaceRendererCompat(renderer)
              tag = compat
              rendererCompat = compat
            }
          },
          update = { v ->
            updateView(v)
          },
          modifier = modifier.onGloballyPositioned { videoSinkVisibility.onGloballyPositioned(it) },
      )
    }

    RendererType.Texture -> {
      AndroidView(
          factory = { context ->
            FrameLayout(context).apply {
              val renderer =
                  TextureViewRenderer(context).apply {
                    init(room.eglBaseContext, rendererEvents)
                    setupVideoIfNeeded(videoTrack, this)
                  }
              addView(
                  renderer,
                  FrameLayout.LayoutParams(
                      FrameLayout.LayoutParams.WRAP_CONTENT,
                      FrameLayout.LayoutParams.WRAP_CONTENT,
                      Gravity.CENTER,
                  ),
              )

              val compat = TextureRendererCompat(renderer)
              tag = compat
              rendererCompat = compat
            }
          },
          update = { v -> updateView(v) },
          modifier = modifier.onGloballyPositioned { videoSinkVisibility.onGloballyPositioned(it) },
      )
    }
  }
}

public enum class RendererType {
  /**
   * Use a [SurfaceView] for rendering. This is more energy efficient and performant, but may have
   * issues with view modifiers (i.e. clipping, rotating, scaling).
   */
  Surface,

  /**
   * Use a [TextureView] for rendering. This is more flexible with composing and various view
   * modifiers (i.e. clipping, rotating, scaling), but is less efficient and may cause more battery
   * drain.
   */
  Texture,
}

private interface RendererCompat {
  val view: View
  val sink: VideoSink

  fun setScalingType(type: RendererCommon.ScalingType)

  fun setMirror(mirror: Boolean)

  fun release()
}

private class SurfaceRendererCompat(override val view: SurfaceViewRenderer) : RendererCompat {
  override val sink: VideoSink
    get() = view

  override fun setScalingType(type: RendererCommon.ScalingType) {
    view.setScalingType(type)
  }

  override fun setMirror(mirror: Boolean) {
    view.setMirror(mirror)
  }

  override fun release() {
    view.release()
  }
}

private class TextureRendererCompat(override val view: TextureViewRenderer) : RendererCompat {
  override val sink: VideoSink
    get() = view

  override fun setScalingType(type: RendererCommon.ScalingType) {
    view.setScalingType(type)
  }

  override fun setMirror(mirror: Boolean) {
    view.setMirror(mirror)
  }

  override fun release() {
    view.release()
  }
}
