/** LiveKit 2.22.3 → Kotlin/JS. All generation choices live here.
 * Run: npm run generate. Output: generated/livekit/*.kt, package livekit.
 * source is matched by compiler type/declaration identity, never assignability.
 * mutable defaults to never (all val); objectConstructor explicitly selects
 * named-argument factories. JS constructors are opted into separately.
 * Unlisted unions/types are errors. Platform contains existing Kotlin mappings.
 * Track-family generic arguments are intentionally erased in this iteration.
 */
import type * as LK from 'livekit-client';
import type * as LKProto from '@livekit/protocol';

import { default as TypedPromise } from './node_modules/livekit-client/dist/src/utils/TypedPromise';

type Class<
  T,
  Members extends keyof T,
  Events extends { [K in keyof Events]: (...args: any[]) => void } = {},
  Mutable extends Members = never,
> = {
  mutable: Mutable;
  kind: 'class';
  source: T;
  members: Pick<T, Members>;
  events: Events;
};

type Subclass<
  Parent extends { kind: 'class'; source: object },
  T extends Parent['source'],
  Members extends keyof T,
  Events extends { [K in keyof Events]: (...args: any[]) => void } = {},
  Mutable extends Members = never,
> = Class<T, Members, Events, Mutable> & { parent: Parent };

type Interface<
  T,
  Members extends keyof T,
  Mutable extends Members = never,
  Construct extends boolean = true,
> = {
  mutable: Mutable;
  objectConstructor: Construct;
  kind: 'interface';
  source: T;
  members: Pick<T, Members>;
};

// Emit all enum cases, not a Kotlin enum class with unrelated JS values.
type Enum<T> = { kind: 'enum'; source: T };
type Union<T> = { kind: 'union'; source: T };
type Native<T, Name extends string> = { source: T; kotlin: Name };

type RoomWithRoomInfo = Omit<LK.Room, 'roomInfo'> & {
  roomInfo?: LKProto.Room;
};

type ExposedBaseKeyProvider = Omit<LK.BaseKeyProvider, 'onSetEncryptionKey'> & {
  onSetEncryptionKey(key: CryptoKey, participantIdentity?: string, keyIndex?: number): void;
};

export type Bindings = {
  RoomModel: Interface<LKProto.Room, 'sid'>;
  // Room and participant hierarchy.
  Room: Class<
    RoomWithRoomInfo,
    | 'roomInfo'
    | 'state'
    | 'name'
    | 'metadata'
    | 'numParticipants'
    | 'isRecording'
    | 'localParticipant'
    | 'remoteParticipants'
    | 'activeSpeakers'
    | 'isE2EEEnabled'
    | 'connect'
    | 'disconnect'
    | 'prepareConnection'
    | 'getParticipantByIdentity'
    | 'startAudio'
    | 'startVideo'
    | 'canPlaybackAudio'
    | 'canPlaybackVideo'
    | 'getActiveDevice'
    | 'getSid'
    | 'options'
    | 'switchActiveDevice',
    Pick<
      LK.RoomEventCallbacks,
      Exclude<
        keyof LK.RoomEventCallbacks,
        | 'dataReceived'
        | 'chatMessage'
        | 'transcriptionReceived'
        | 'dcBufferStatusChanged'
        | 'sipDTMFReceived'
        | 'metricsReceived'
        | 'dataTrackPublished'
        | 'dataTrackUnpublished'
        | 'localDataTrackPublished'
        | 'localDataTrackUnpublished'
      >
    >
  > & {
    // Inspect construct signatures ONLY; do not export all static properties.
    constructors: typeof LK.Room;
  };

  Participant: Class<
    LK.Participant,
    | 'sid'
    | 'identity'
    | 'name'
    | 'metadata'
    | 'attributes'
    | 'kind'
    | 'audioLevel'
    | 'isSpeaking'
    | 'lastSpokeAt'
    | 'joinedAt'
    | 'connectionQuality'
    | 'isLocal'
    | 'permissions'
    | 'isCameraEnabled'
    | 'isMicrophoneEnabled'
    | 'isScreenShareEnabled'
    | 'trackPublications'
    | 'audioTrackPublications'
    | 'videoTrackPublications'
    | 'getTrackPublication'
    | 'getTrackPublicationByName'
    | 'getTrackPublications',
    Pick<
      LK.ParticipantEventCallbacks,
      | 'isSpeakingChanged'
      | 'connectionQualityChanged'
      | 'participantMetadataChanged'
      | 'participantNameChanged'
      | 'participantPermissionsChanged'
      | 'attributesChanged'
      | 'localTrackSubscribed'
      | 'trackMuted'
      | 'trackUnmuted'
      | 'trackPublished'
      | 'trackSubscribed'
      | 'trackUnpublished'
      | 'trackUnsubscribed'
      | 'trackStreamStateChanged'
      | 'trackSubscriptionPermissionChanged'
      | 'trackSubscriptionFailed'
      | 'trackSubscriptionStatusChanged'
      | 'localTrackPublished'
      | 'localTrackUnpublished'
    >
  >;

  ParticipantPermissions: Class<
    NonNullable<LK.Participant['permissions']>,
    | 'canPublish'
    | 'canSubscribe'
    | 'canPublishData'
    | 'hidden'
    | 'recorder'
    | 'canPublishSources'
    | 'canUpdateMetadata'
    | 'canSubscribeMetrics'
  >;

  ProtocolTrackSource: Enum<
    NonNullable<LK.Participant['permissions']>['canPublishSources'][number]
  >;

  LocalParticipant: Subclass<
    Bindings['Participant'],
    LK.LocalParticipant,
    | 'getTrackPublication'
    | 'getTrackPublicationByName'
    | 'setName'
    | 'setMetadata'
    | 'setAttributes'
    | 'publishDtmf'
    | 'setCameraEnabled'
    | 'setMicrophoneEnabled'
    | 'setScreenShareEnabled'
    | 'enableCameraAndMicrophone'
    | 'createTracks'
    | 'createScreenTracks'
    | 'publishTrack'
    | 'unpublishTrack'
    | 'setE2EEEnabled'
    | 'publishData'
    | 'lastCameraError'
    | 'lastMicrophoneError',
    Pick<
      LK.ParticipantEventCallbacks,
      'localTrackPublished' | 'localTrackUnpublished' | 'mediaDevicesError'
    >
  >;

  RemoteParticipant: Subclass<
    Bindings['Participant'],
    LK.RemoteParticipant,
    'getTrackPublication' | 'getTrackPublicationByName' | 'setVolume' | 'getVolume',
    Pick<
      LK.ParticipantEventCallbacks,
      'trackPublished' | 'trackUnpublished' | 'trackSubscribed' | 'trackUnsubscribed'
    >
  >;

  // Track hierarchy. Generic SDK Track<Kind> types project to these bindings.
  Track: Class<
    LK.Track,
    | 'kind'
    | 'source'
    | 'sid'
    | 'isMuted'
    | 'isLocal'
    | 'streamState'
    | 'mediaStreamTrack'
    | 'attachedElements'
    | 'currentBitrate'
    | 'attach'
    | 'detach'
    | 'stop',
    // Several SDK track callbacks use any; leave those out in this iteration.
    Pick<
      LK.TrackEventCallbacks,
      | 'elementAttached'
      | 'elementDetached'
      | 'audioPlaybackStarted'
      | 'audioPlaybackFailed'
      | 'videoPlaybackStarted'
      | 'videoPlaybackFailed'
      | 'videoDimensionsChanged'
      | 'visibilityChanged'
    >
  >;

  LocalTrack: Subclass<
    Bindings['Track'],
    LK.LocalTrack,
    'mute' | 'unmute' | 'pauseUpstream' | 'resumeUpstream' | 'isUpstreamPaused'
  >;

  RemoteTrack: Subclass<
    Bindings['Track'],
    LK.RemoteTrack,
    'start' | 'setPlayoutDelay' | 'getPlayoutDelay'
  >;

  LocalAudioTrack: Subclass<
    Bindings['LocalTrack'],
    LK.LocalAudioTrack,
    'restartTrack' | 'checkForSilence' | 'stopOnMute' | 'enhancedNoiseCancellation'
  >;

  LocalVideoTrack: Subclass<
    Bindings['LocalTrack'],
    LK.LocalVideoTrack,
    'restartTrack' | 'isSimulcast' | 'setPublishingQuality'
  >;

  RemoteAudioTrack: Subclass<
    Bindings['RemoteTrack'],
    LK.RemoteAudioTrack,
    'setVolume' | 'getVolume' | 'setSinkId'
  >;

  RemoteVideoTrack: Subclass<
    Bindings['RemoteTrack'],
    LK.RemoteVideoTrack,
    'isAdaptiveStream' | 'getDecoderImplementation'
  >;

  // Publications describe published tracks, including unsubscribed ones.
  TrackPublication: Class<
    LK.TrackPublication,
    | 'trackSid'
    | 'trackName'
    | 'kind'
    | 'source'
    | 'track'
    | 'mimeType'
    | 'dimensions'
    | 'simulcasted'
    | 'isMuted'
    | 'isEnabled'
    | 'isSubscribed'
    | 'isLocal'
    | 'audioTrack'
    | 'videoTrack',
    // Pick<LK.PublicationEventCallbacks, 'muted' | 'unmuted' | 'ended'>
    Pick<
      LK.PublicationEventCallbacks,
      | 'muted'
      | 'unmuted'
      | 'ended'
      | 'subscribed'
      | 'subscriptionFailed'
      | 'unsubscribed'
      | 'subscriptionStatusChanged'
      | 'subscriptionPermissionChanged'
      //   | 'updateSettings'
      //   | 'subscriptionPermissionChanged'
      //   | 'updateSubscription'
      //   | 'subscribed'
      //   | 'unsubscribed'
      //   | 'subscriptionStatusChanged'
      //   | 'subscriptionFailed'
      //   | 'transcriptionReceived'
      //   | 'timeSyncUpdate'
      //   | 'cpuConstrained'
    >
  >;

  LocalTrackPublication: Subclass<
    Bindings['TrackPublication'],
    LK.LocalTrackPublication,
    'options' | 'mute' | 'unmute' | 'pauseUpstream' | 'resumeUpstream' | 'isUpstreamPaused'
  >;

  RemoteTrackPublication: Subclass<
    Bindings['TrackPublication'],
    LK.RemoteTrackPublication,
    | 'setSubscribed'
    | 'setEnabled'
    | 'setVideoQuality'
    | 'setVideoDimensions'
    | 'videoQuality'
    | 'setVideoFPS'
    | 'isDesired'
    | 'subscriptionStatus'
    | 'permissionStatus'
    // Pick<
    //   LK.PublicationEventCallbacks,
    //   'subscribed' | 'unsubscribed' | 'subscriptionStatusChanged' | 'subscriptionPermissionChanged'
    // >
  >;

  // Enum dependencies. Flatten nested SDK names for Kotlin naming only.
  ConnectionState: Enum<LK.ConnectionState>;
  ConnectionQuality: Enum<LK.ConnectionQuality>;
  ParticipantKind: Enum<LK.ParticipantKind>;
  TrackKind: Enum<LK.Track.Kind>;
  TrackSource: Enum<LK.Track.Source>;
  TrackStreamState: Enum<LK.Track.StreamState>;
  VideoQuality: Enum<LK.VideoQuality>;
  SubscriptionStatus: Enum<LK.TrackPublication.SubscriptionStatus>;
  PermissionStatus: Enum<LK.TrackPublication.PermissionStatus>;
  DisconnectReason: Enum<LK.DisconnectReason>;
  DataPacketKind: Enum<LK.DataPacket_Kind>;
  EncryptionType: Enum<LK.Encryption_Type>;

  // Input objects: only selected fields are exposed, retaining optionality.
  RoomOptions: Interface<
    LK.RoomOptions,
    | 'adaptiveStream'
    | 'dynacast'
    | 'audioCaptureDefaults'
    | 'videoCaptureDefaults'
    | 'stopLocalTrackOnUnpublish'
    | 'disconnectOnPageLeave'
    | 'reconnectPolicy'
    | 'loggerName'
    | 'encryption'
  >;

  InternalRoomOptions: Interface<LK.InternalRoomOptions, 'encryption'>;

  E2EEOptions: Union<LK.E2EEOptions>;

  E2EEManagerOptions: Interface<LK.E2EEManagerOptions, 'keyProvider' | 'worker'>;

  ManagedE2EEOptions: Interface<Extract<LK.E2EEOptions, { e2eeManager: unknown }>, never>;

  BaseKeyProvider: Class<LK.BaseKeyProvider, never> & {
    constructors: typeof LK.BaseKeyProvider;
  };
  PartialKeyProviderOptions: Interface<
    Partial<LK.KeyProviderOptions>,
    | 'sharedKey'
    | 'ratchetWindowSize'
    | 'ratchetSalt'
    | 'keyringSize'
    | 'keySize'
    | 'failureTolerance'
  >;
  KeySize: Union<LK.KeyProviderOptions['keySize']>;

  RoomConnectOptions: Interface<
    LK.RoomConnectOptions,
    'autoSubscribe' | 'maxRetries' | 'peerConnectionTimeout' | 'websocketTimeout'
  >;

  CreateLocalTracksOptions: Interface<LK.CreateLocalTracksOptions, 'audio' | 'video'>;

  AudioCaptureOptions: Interface<
    LK.AudioCaptureOptions,
    | 'deviceId'
    | 'autoGainControl'
    | 'echoCancellation'
    | 'noiseSuppression'
    | 'channelCount'
    | 'sampleRate'
  >;

  VideoCaptureOptions: Interface<
    LK.VideoCaptureOptions,
    'deviceId' | 'frameRate' | 'facingMode' | 'resolution'
  >;

  ScreenShareCaptureOptions: Interface<
    LK.ScreenShareCaptureOptions,
    | 'audio'
    | 'resolution'
    | 'systemAudio'
    | 'selfBrowserSurface'
    | 'surfaceSwitching'
    | 'preferCurrentTab'
  >;

  TrackPublishOptions: Interface<
    LK.TrackPublishOptions,
    | 'name'
    | 'source'
    | 'stream'
    | 'simulcast'
    | 'dtx'
    | 'red'
    | 'forceStereo'
    | 'stopMicTrackOnMute'
  >;

  DataPublishOptions: Interface<
    LK.DataPublishOptions,
    'reliable' | 'topic' | 'destinationIdentities'
  >;

  VideoResolution: Interface<LK.VideoResolution, 'width' | 'height' | 'frameRate' | 'aspectRatio'>;

  TrackDimensions: Interface<LK.Track.Dimensions, 'width' | 'height'>;

  // Not publicly re-exported as a named type: preserve its actual SDK identity
  // through indexed access, without importing private SDK paths.
  AdaptiveStreamSettings: Interface<
    Exclude<NonNullable<LK.RoomOptions['adaptiveStream']>, boolean>,
    'pixelDensity' | 'pauseVideoInBackground'
  >;

  ReconnectPolicy: Interface<LK.ReconnectPolicy, 'nextRetryDelayInMs'>;
  ReconnectContext: Interface<LK.ReconnectContext, 'elapsedMs' | 'retryCount'>;

  // Explicit union and literal choices; no guessing in cli.ts.
  ParticipantChoice: Union<LK.LocalParticipant | LK.RemoteParticipant>;
  PublishableTrack: Union<LK.LocalTrack | MediaStreamTrack>;
  AudioTrackChoice: Union<LK.LocalAudioTrack | LK.RemoteAudioTrack>;
  VideoTrackChoice: Union<LK.LocalVideoTrack | LK.RemoteVideoTrack>;
  AdaptiveStreamChoice: Union<NonNullable<LK.RoomOptions['adaptiveStream']>>;
  AudioCaptureChoice: Union<NonNullable<LK.CreateLocalTracksOptions['audio']>>;
  VideoCaptureChoice: Union<NonNullable<LK.CreateLocalTracksOptions['video']>>;
  DeviceConstraint: Union<ConstrainDOMString>;
  BooleanConstraint: Union<ConstrainBoolean>;
  IntegerConstraint: Union<ConstrainULong>;
  NumberConstraint: Union<ConstrainDouble>;
  PixelDensity: Union<NonNullable<Bindings['AdaptiveStreamSettings']['source']['pixelDensity']>>;
  MediaDeviceKind: Enum<MediaDeviceKind>;
  FacingMode: Enum<NonNullable<LK.VideoCaptureOptions['facingMode']>>;
  SurfaceChoice: Enum<NonNullable<LK.ScreenShareCaptureOptions['systemAudio']>>;
  AudioTrackSource: Enum<LK.Track.Source.Microphone | LK.Track.Source.ScreenShareAudio>;

  SubscriptionError: Enum<LK.SubscriptionError>;
};

// Existing platform types, not files to generate. Generic mappings use the
// Kotlin name with the original type arguments. No structural SDK matching.
export type Platform = {
  Promise: Native<Promise<unknown>, 'Promise<{0}>'>;
  TypedPromise: Native<TypedPromise<unknown, Error>, 'Promise<{0}>'>;
  Array: Native<Array<unknown>, 'JsArray<{0}>'>;
  Map: Native<Map<unknown, unknown>, 'JsMap<{0}, {1}>'>;
  Record: Native<Record<string, unknown>, 'JsRecord<{0}>'>;
  Date: Native<Date, 'Date'>;
  Error: Native<Error, 'JsError'>;
  HTMLMediaElement: Native<HTMLMediaElement, 'org.w3c.dom.HTMLMediaElement'>;
  MediaStreamTrack: Native<MediaStreamTrack, 'org.w3c.dom.mediacapture.MediaStreamTrack'>;
  Uint8Array: Native<Uint8Array, 'org.khronos.webgl.Uint8Array'>;
  StringConstraint: Native<
    ConstrainDOMStringParameters,
    'org.w3c.dom.mediacapture.ConstrainDOMStringParameters'
  >;
  BooleanConstraint: Native<
    ConstrainBooleanParameters,
    'org.w3c.dom.mediacapture.ConstrainBooleanParameters'
  >;
  DoubleConstraint: Native<ConstrainDoubleRange, 'org.w3c.dom.mediacapture.ConstrainDoubleRange'>;
  IntegerConstraint: Native<ConstrainULongRange, 'org.w3c.dom.mediacapture.ConstrainULongRange'>;
  Worker: Native<Worker, 'org.w3c.dom.Worker'>;
  CryptoKey: Native<CryptoKey, 'CryptoKey'>;
};
