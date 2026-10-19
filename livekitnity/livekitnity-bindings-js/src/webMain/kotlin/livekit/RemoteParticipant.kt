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

public external interface RemoteParticipant : Participant {
  public override fun `getTrackPublication`(`source`: TrackSource): RemoteTrackPublication?

  public override fun `getTrackPublicationByName`(`name`: String): RemoteTrackPublication?

  public fun `setVolume`(`volume`: Double, `source`: AudioTrackSource? = definedExternally): Unit

  public fun `getVolume`(`source`: AudioTrackSource? = definedExternally): Double?
}

public object RemoteParticipantEvent {
  public val TrackPublished: EventCallback1<RemoteTrackPublication> =
      eventCallback1("trackPublished")
  public val TrackUnpublished: EventCallback1<RemoteTrackPublication> =
      eventCallback1("trackUnpublished")
  public val TrackSubscribed: EventCallback2<RemoteTrack, RemoteTrackPublication> =
      eventCallback2("trackSubscribed")
  public val TrackUnsubscribed: EventCallback2<RemoteTrack, RemoteTrackPublication> =
      eventCallback2("trackUnsubscribed")
}
