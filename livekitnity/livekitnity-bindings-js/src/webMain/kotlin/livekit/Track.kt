@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress(
    // "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "RemoveRedundantBackticks",
    "RemoveExplicitTypeArguments",
    "RedundantUnitReturnType",
    "UnnecessaryOptInAnnotation",
    // "unused",
)

package livekit

import kotlin.js.*

public external interface Track : EventEmitter {
  public val `source`: TrackSource

  public fun `stop`(): Unit

  public val `kind`: TrackKind
  public val `sid`: String?
  public val `isLocal`: Boolean
  public val `isMuted`: Boolean
  public val `streamState`: TrackStreamState
  public val `mediaStreamTrack`: org.w3c.dom.mediacapture.MediaStreamTrack
  public val `attachedElements`: JsArray<org.w3c.dom.HTMLMediaElement>
  public val `currentBitrate`: Double

  public fun `attach`(): org.w3c.dom.HTMLMediaElement

  public fun `attach`(`element`: org.w3c.dom.HTMLMediaElement): org.w3c.dom.HTMLMediaElement

  public fun `detach`(): JsArray<org.w3c.dom.HTMLMediaElement>

  public fun `detach`(`element`: org.w3c.dom.HTMLMediaElement): org.w3c.dom.HTMLMediaElement
}

public object TrackEvent {
  public val AudioPlaybackStarted: EventCallback0 = eventCallback0("audioPlaybackStarted")
  public val AudioPlaybackFailed: EventCallback1<JsError?> = eventCallback1("audioPlaybackFailed")
  public val VisibilityChanged: EventCallback2<JsBoolean, JsAny> =
      eventCallback2("visibilityChanged")
  public val VideoDimensionsChanged: EventCallback2<TrackDimensions, JsAny> =
      eventCallback2("videoDimensionsChanged")
  public val VideoPlaybackStarted: EventCallback0 = eventCallback0("videoPlaybackStarted")
  public val VideoPlaybackFailed: EventCallback1<JsError?> = eventCallback1("videoPlaybackFailed")
  public val ElementAttached: EventCallback1<org.w3c.dom.HTMLMediaElement> =
      eventCallback1("elementAttached")
  public val ElementDetached: EventCallback1<org.w3c.dom.HTMLMediaElement> =
      eventCallback1("elementDetached")
}
