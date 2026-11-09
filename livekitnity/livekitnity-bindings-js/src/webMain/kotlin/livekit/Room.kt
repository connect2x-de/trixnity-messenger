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

public external interface Room : EventEmitter {
  public val `name`: String
  public val `state`: ConnectionState
  public val `metadata`: String?
  public val `options`: InternalRoomOptions

  public fun `connect`(
      `url`: String,
      `token`: String,
      `opts`: RoomConnectOptions? = definedExternally,
  ): Promise<Nothing?>

  public fun `disconnect`(`stopTracks`: Boolean? = definedExternally): Promise<Nothing?>

  public val `roomInfo`: RoomModel?
  public val `remoteParticipants`: JsMap<JsString, RemoteParticipant>
  public val `activeSpeakers`: JsArray<Participant>
  public val `localParticipant`: LocalParticipant
  public val `isE2EEEnabled`: Boolean
  public val `isRecording`: Boolean

  public fun `getSid`(): Promise<JsString>

  public val `numParticipants`: Double

  public fun `prepareConnection`(
      `url`: String,
      `token`: String? = definedExternally,
  ): Promise<Nothing?>

  public fun `getParticipantByIdentity`(`identity`: String): Participant?

  public fun `startAudio`(): Promise<Nothing?>

  public fun `startVideo`(): Promise<Nothing?>

  public val `canPlaybackAudio`: Boolean
  public val `canPlaybackVideo`: Boolean

  public fun `getActiveDevice`(`kind`: MediaDeviceKind): String?

  public fun `switchActiveDevice`(
      `kind`: MediaDeviceKind,
      `deviceId`: String,
      `exact`: Boolean? = definedExternally,
  ): Promise<JsBoolean>
}

@JsModule("livekit-client")
private external object RoomModule {
  val `Room`: Constructor<Room>
}

public fun Room(`options`: RoomOptions? = undefined()): Room {
  return construct(RoomModule.`Room`, options)
}

public object RoomEvent {
  public val Connected: EventCallback0 = eventCallback0("connected")
  public val Disconnected: EventCallback1<DisconnectReason?> = eventCallback1("disconnected")
  public val TrackPublished: EventCallback2<RemoteTrackPublication, RemoteParticipant> =
      eventCallback2("trackPublished")
  public val TrackUnpublished: EventCallback2<RemoteTrackPublication, RemoteParticipant> =
      eventCallback2("trackUnpublished")
  public val TrackSubscribed:
      EventCallback3<RemoteTrack, RemoteTrackPublication, RemoteParticipant> =
      eventCallback3("trackSubscribed")
  public val LocalTrackUnpublished: EventCallback2<LocalTrackPublication, LocalParticipant> =
      eventCallback2("localTrackUnpublished")
  public val LocalTrackSubscribed: EventCallback2<LocalTrackPublication, LocalParticipant> =
      eventCallback2("localTrackSubscribed")
  public val SignalConnected: EventCallback0 = eventCallback0("signalConnected")
  public val TrackSubscriptionFailed:
      EventCallback3<JsString, RemoteParticipant, SubscriptionError?> =
      eventCallback3("trackSubscriptionFailed")
  public val TrackUnsubscribed:
      EventCallback3<RemoteTrack, RemoteTrackPublication, RemoteParticipant> =
      eventCallback3("trackUnsubscribed")
  public val TrackMuted: EventCallback2<TrackPublication, Participant> =
      eventCallback2("trackMuted")
  public val TrackUnmuted: EventCallback2<TrackPublication, Participant> =
      eventCallback2("trackUnmuted")
  public val LocalTrackPublished: EventCallback2<LocalTrackPublication, LocalParticipant> =
      eventCallback2("localTrackPublished")
  public val ParticipantMetadataChanged: EventCallback2<JsString?, ParticipantChoice> =
      eventCallback2("participantMetadataChanged")
  public val ParticipantNameChanged: EventCallback2<JsString, ParticipantChoice> =
      eventCallback2("participantNameChanged")
  public val ConnectionQualityChanged: EventCallback2<ConnectionQuality, Participant> =
      eventCallback2("connectionQualityChanged")
  public val TrackStreamStateChanged:
      EventCallback3<RemoteTrackPublication, TrackStreamState, RemoteParticipant> =
      eventCallback3("trackStreamStateChanged")
  public val TrackSubscriptionPermissionChanged:
      EventCallback3<RemoteTrackPublication, PermissionStatus, RemoteParticipant> =
      eventCallback3("trackSubscriptionPermissionChanged")
  public val MediaDevicesError: EventCallback2<JsError, MediaDeviceKind?> =
      eventCallback2("mediaDevicesError")
  public val ParticipantPermissionsChanged:
      EventCallback2<ParticipantPermissions?, ParticipantChoice> =
      eventCallback2("participantPermissionsChanged")
  public val TrackSubscriptionStatusChanged:
      EventCallback3<RemoteTrackPublication, SubscriptionStatus, RemoteParticipant> =
      eventCallback3("trackSubscriptionStatusChanged")
  public val ParticipantEncryptionStatusChanged: EventCallback2<JsBoolean, Participant?> =
      eventCallback2("participantEncryptionStatusChanged")
  public val EncryptionError: EventCallback2<JsError, Participant?> =
      eventCallback2("encryptionError")
  public val Reconnecting: EventCallback0 = eventCallback0("reconnecting")
  public val SignalReconnecting: EventCallback0 = eventCallback0("signalReconnecting")
  public val Reconnected: EventCallback0 = eventCallback0("reconnected")
  public val ConnectionStateChanged: EventCallback1<ConnectionState> =
      eventCallback1("connectionStateChanged")
  public val Moved: EventCallback1<JsString> = eventCallback1("moved")
  public val MediaDevicesChanged: EventCallback0 = eventCallback0("mediaDevicesChanged")
  public val ParticipantConnected: EventCallback1<RemoteParticipant> =
      eventCallback1("participantConnected")
  public val ParticipantDisconnected: EventCallback2<RemoteParticipant, DisconnectReason?> =
      eventCallback2("participantDisconnected")
  public val LocalAudioSilenceDetected: EventCallback1<LocalTrackPublication> =
      eventCallback1("localAudioSilenceDetected")
  public val ParticipantAttributesChanged: EventCallback2<JsRecord<JsString>, ParticipantChoice> =
      eventCallback2("participantAttributesChanged")
  public val ActiveSpeakersChanged: EventCallback1<JsArray<Participant>> =
      eventCallback1("activeSpeakersChanged")
  public val RoomMetadataChanged: EventCallback1<JsString> = eventCallback1("roomMetadataChanged")
  public val AudioPlaybackChanged: EventCallback1<JsBoolean> =
      eventCallback1("audioPlaybackChanged")
  public val VideoPlaybackChanged: EventCallback1<JsBoolean> =
      eventCallback1("videoPlaybackChanged")
  public val RecordingStatusChanged: EventCallback1<JsBoolean> =
      eventCallback1("recordingStatusChanged")
  public val ActiveDeviceChanged: EventCallback2<MediaDeviceKind, JsString> =
      eventCallback2("activeDeviceChanged")
  public val ParticipantActive: EventCallback1<Participant> = eventCallback1("participantActive")
}
