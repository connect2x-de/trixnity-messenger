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

public external interface Participant : EventEmitter {
  public val `name`: String?
  public val `metadata`: String?
  public val `attributes`: JsRecord<JsString>
  public val `kind`: ParticipantKind
  public val `connectionQuality`: ConnectionQuality
  public val `identity`: String
  public val `sid`: String
  public val `audioLevel`: Double
  public val `isSpeaking`: Boolean
  public val `lastSpokeAt`: Date?
  public val `joinedAt`: Date?
  public val `isLocal`: Boolean
  public val `permissions`: ParticipantPermissions?
  public val `isCameraEnabled`: Boolean
  public val `isMicrophoneEnabled`: Boolean
  public val `isScreenShareEnabled`: Boolean
  public val `trackPublications`: JsMap<JsString, TrackPublication>
  public val `audioTrackPublications`: JsMap<JsString, TrackPublication>
  public val `videoTrackPublications`: JsMap<JsString, TrackPublication>

  public fun `getTrackPublication`(`source`: TrackSource): TrackPublication?

  public fun `getTrackPublicationByName`(`name`: String): TrackPublication?

  public fun `getTrackPublications`(): JsArray<TrackPublication>
}

public object ParticipantEvent {
  public val TrackPublished: EventCallback1<RemoteTrackPublication> =
      eventCallback1("trackPublished")
  public val TrackUnpublished: EventCallback1<RemoteTrackPublication> =
      eventCallback1("trackUnpublished")
  public val TrackSubscribed: EventCallback2<RemoteTrack, RemoteTrackPublication> =
      eventCallback2("trackSubscribed")
  public val LocalTrackUnpublished: EventCallback1<LocalTrackPublication> =
      eventCallback1("localTrackUnpublished")
  public val LocalTrackSubscribed: EventCallback1<LocalTrackPublication> =
      eventCallback1("localTrackSubscribed")
  public val TrackSubscriptionFailed: EventCallback2<JsString, SubscriptionError?> =
      eventCallback2("trackSubscriptionFailed")
  public val TrackUnsubscribed: EventCallback2<RemoteTrack, RemoteTrackPublication> =
      eventCallback2("trackUnsubscribed")
  public val TrackMuted: EventCallback1<TrackPublication> = eventCallback1("trackMuted")
  public val TrackUnmuted: EventCallback1<TrackPublication> = eventCallback1("trackUnmuted")
  public val LocalTrackPublished: EventCallback1<LocalTrackPublication> =
      eventCallback1("localTrackPublished")
  public val ParticipantMetadataChanged: EventCallback2<JsString?, JsAny> =
      eventCallback2("participantMetadataChanged")
  public val ParticipantNameChanged: EventCallback1<JsString> =
      eventCallback1("participantNameChanged")
  public val IsSpeakingChanged: EventCallback1<JsBoolean> = eventCallback1("isSpeakingChanged")
  public val ConnectionQualityChanged: EventCallback1<ConnectionQuality> =
      eventCallback1("connectionQualityChanged")
  public val TrackStreamStateChanged: EventCallback2<RemoteTrackPublication, TrackStreamState> =
      eventCallback2("trackStreamStateChanged")
  public val TrackSubscriptionPermissionChanged:
      EventCallback2<RemoteTrackPublication, PermissionStatus> =
      eventCallback2("trackSubscriptionPermissionChanged")
  public val ParticipantPermissionsChanged: EventCallback1<ParticipantPermissions?> =
      eventCallback1("participantPermissionsChanged")
  public val TrackSubscriptionStatusChanged:
      EventCallback2<RemoteTrackPublication, SubscriptionStatus> =
      eventCallback2("trackSubscriptionStatusChanged")
  public val AttributesChanged: EventCallback1<JsRecord<JsString>> =
      eventCallback1("attributesChanged")
}
