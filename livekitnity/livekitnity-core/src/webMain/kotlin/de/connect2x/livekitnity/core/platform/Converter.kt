package de.connect2x.livekitnity.core.platform

import de.connect2x.livekitnity.api.ConnectOptions
import de.connect2x.livekitnity.api.RoomOptions
import de.connect2x.livekitnity.api.e2ee.E2EEOptions
import de.connect2x.livekitnity.api.e2ee.KeyProvider
import de.connect2x.livekitnity.api.e2ee.KeyProviderOptions
import de.connect2x.livekitnity.api.network.ReconnectContext
import de.connect2x.livekitnity.api.network.ReconnectPolicy
import de.connect2x.livekitnity.api.room.DisconnectReason
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.WebRoom
import de.connect2x.livekitnity.api.room.participant.ConnectionQuality
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.ParticipantPermission
import de.connect2x.livekitnity.api.room.participant.WebLocalParticipant
import de.connect2x.livekitnity.api.room.participant.WebParticipant
import de.connect2x.livekitnity.api.room.participant.WebRemoteParticipant
import de.connect2x.livekitnity.api.room.track.CameraPosition
import de.connect2x.livekitnity.api.room.track.LocalAudioTrackOptions
import de.connect2x.livekitnity.api.room.track.LocalVideoTrackOptions
import de.connect2x.livekitnity.api.room.track.RemoteTrackPublication
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.VideoCaptureParameter
import de.connect2x.livekitnity.api.room.track.VideoQuality
import de.connect2x.livekitnity.api.room.track.WebLocalAudioTrack
import de.connect2x.livekitnity.api.room.track.WebLocalTrackPublication
import de.connect2x.livekitnity.api.room.track.WebLocalVideoTrack
import de.connect2x.livekitnity.api.room.track.WebRemoteAudioTrack
import de.connect2x.livekitnity.api.room.track.WebRemoteTrackPublication
import de.connect2x.livekitnity.api.room.track.WebRemoteVideoTrack
import de.connect2x.livekitnity.api.room.track.WebTrack
import de.connect2x.livekitnity.api.room.track.WebTrackPublication
import de.connect2x.livekitnity.core.e2ee.NativeBaseKeyProvider
import de.connect2x.livekitnity.core.e2ee.WebKeyProvider
import de.connect2x.livekitnity.core.room.WebRoom
import de.connect2x.livekitnity.core.room.participant.WebLocalParticipant
import de.connect2x.livekitnity.core.room.participant.WebRemoteParticipant
import de.connect2x.livekitnity.core.room.track.WebLocalAudioTrack
import de.connect2x.livekitnity.core.room.track.WebLocalTrackPublication
import de.connect2x.livekitnity.core.room.track.WebLocalVideoTrack
import de.connect2x.livekitnity.core.room.track.WebRemoteAudioTrack
import de.connect2x.livekitnity.core.room.track.WebRemoteTrackPublication
import de.connect2x.livekitnity.core.room.track.WebRemoteVideoTrack
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.suspendCancellableCoroutine
import livekit.AdaptiveStreamChoice
import livekit.DeviceConstraint
import livekit.JsRecord
import livekit.RoomModel
import org.w3c.dom.Worker

internal class Converter {
  private val cache = WeakIdentityCache()

  internal fun toCommon(
      room: livekit.Room,
  ): WebRoom {
    return cache.getOrCreate(RoomCacheKeyFn(room)) {
      WebRoom(
          room = room,
          converter = this,
      )
    }
  }

  internal fun toCommon(
      participant: livekit.Participant,
  ): WebParticipant {
    return if (participant.isLocal) {
      toCommon(
          localParticipant = participant.unsafeCast<livekit.LocalParticipant>(),
      )
    } else {
      toCommon(
          remoteParticipant = participant.unsafeCast<livekit.RemoteParticipant>(),
      )
    }
  }

  internal fun toCommon(
      trackPublication: livekit.TrackPublication,
  ): WebTrackPublication {
    return if (trackPublication.isLocal) {
      toCommon(
          localTrackPublication = trackPublication.unsafeCast<livekit.LocalTrackPublication>(),
      )
    } else {
      toCommon(
          remoteTrackPublication = trackPublication.unsafeCast<livekit.RemoteTrackPublication>(),
      )
    }
  }

  internal fun toPartialTrack(track: livekit.Track?): (String) -> WebTrack? {
    return if (track == null) {
      { null }
    } else { trackName -> toCommon(track, trackName) }
  }

  internal fun toCommon(
      track: livekit.Track,
      trackName: String,
  ): WebTrack {
    val kind = toCommon(track.kind)
    return when (kind) {
      Track.Kind.AUDIO ->
          if (track.isLocal) {
            toCommon(
                localAudioTrack = track.unsafeCast<livekit.LocalAudioTrack>(),
                trackName = trackName,
            )
          } else {
            toCommon(
                remoteAudioTrack = track.unsafeCast<livekit.RemoteAudioTrack>(),
                trackName = trackName,
            )
          }

      Track.Kind.VIDEO ->
          if (track.isLocal) {
            toCommon(
                localVideoTrack = track.unsafeCast<livekit.LocalVideoTrack>(),
                trackName = trackName,
            )
          } else {
            toCommon(
                remoteVideoTrack = track.unsafeCast<livekit.RemoteVideoTrack>(),
                trackName = trackName,
            )
          }

      Track.Kind.UNRECOGNIZED -> unreachable()
    }
  }

  internal fun toCommon(
      keyProvider: livekit.BaseKeyProvider,
  ): KeyProvider {
    return cache.getOrCreate(KeyProviderCacheKeyFn(keyProvider)) {
      WebKeyProvider(
          keyProvider = keyProvider.unsafeCast<NativeBaseKeyProvider>(),
      )
    }
  }

  internal fun toCommon(
      localParticipant: livekit.LocalParticipant,
  ): WebLocalParticipant {
    return cache.getOrCreate(LocalParticipantCacheKeyFn(localParticipant)) {
      WebLocalParticipant(
          localParticipant = localParticipant,
          converter = this,
      )
    }
  }

  internal fun toCommon(
      remoteParticipant: livekit.RemoteParticipant,
  ): WebRemoteParticipant {
    return cache.getOrCreate(RemoteParticipantCacheKeyFn(remoteParticipant)) {
      WebRemoteParticipant(
          remoteParticipant = remoteParticipant,
          converter = this,
      )
    }
  }

  internal fun toCommon(
      localTrackPublication: livekit.LocalTrackPublication,
  ): WebLocalTrackPublication {
    return cache.getOrCreate(LocalTrackPublicationCacheKeyFn(localTrackPublication)) {
      WebLocalTrackPublication(
          localTrackPublication = localTrackPublication,
          converter = this,
      )
    }
  }

  internal fun toCommon(
      remoteTrackPublication: livekit.RemoteTrackPublication,
  ): WebRemoteTrackPublication {
    return cache.getOrCreate(RemoteTrackPublicationCacheKeyFn(remoteTrackPublication)) {
      WebRemoteTrackPublication(
          remoteTrackPublication = remoteTrackPublication,
          converter = this,
      )
    }
  }

  internal fun toCommon(
      localAudioTrack: livekit.LocalAudioTrack,
      trackName: String,
  ): WebLocalAudioTrack {
    return cache.getOrCreate(LocalAudioTrackCacheKeyFn(localAudioTrack)) {
      WebLocalAudioTrack(
          localAudioTrack = localAudioTrack,
          converter = this,
          trackName = trackName,
      )
    }
  }

  internal fun toCommon(
      remoteAudioTrack: livekit.RemoteAudioTrack,
      trackName: String,
  ): WebRemoteAudioTrack {
    return cache.getOrCreate(RemoteAudioTrackCacheKeyFn(remoteAudioTrack)) {
      WebRemoteAudioTrack(
          remoteAudioTrack = remoteAudioTrack,
          converter = this,
          trackName = trackName,
      )
    }
  }

  internal fun toCommon(
      localVideoTrack: livekit.LocalVideoTrack,
      trackName: String,
  ): WebLocalVideoTrack {
    return cache.getOrCreate(LocalVideoTrackCacheKeyFn(localVideoTrack)) {
      WebLocalVideoTrack(
          localVideoTrack = localVideoTrack,
          converter = this,
          trackName = trackName,
      )
    }
  }

  internal fun toCommon(
      remoteVideoTrack: livekit.RemoteVideoTrack,
      trackName: String,
  ): WebRemoteVideoTrack {
    return cache.getOrCreate(RemoteVideoTrackCacheKeyFn(remoteVideoTrack)) {
      WebRemoteVideoTrack(
          remoteVideoTrack = remoteVideoTrack,
          converter = this,
          trackName = trackName,
      )
    }
  }

  internal fun toCommon(
      remoteParticipants: livekit.JsMap<JsString, livekit.RemoteParticipant>,
  ): Map<Participant.Identity, WebRemoteParticipant> {
    return remoteParticipants.asSequence().associate { (identity, remoteParticipant) ->
      toParticipantIdentity(toKotlinString(identity)) to toCommon(remoteParticipant)
    }
  }

  internal fun toCommon(
      participants: JsArray<livekit.Participant>,
  ): List<WebParticipant> {
    return participants
        .asSequence()
        .map { participant ->
          toCommon(
              participant = participant,
          )
        }
        .toList()
  }

  internal fun toCommon(
      trackPublications: livekit.JsMap<JsString, livekit.TrackPublication>,
  ): Map<String, WebTrackPublication> {
    return trackPublications.asSequence().associate { (name, trackPublication) ->
      Pair(
          first = toKotlinString(name),
          second =
              toCommon(
                  trackPublication = trackPublication,
              ),
      )
    }
  }

  internal fun clearCache() {
    cache.clear()
  }

  companion object {

    private val RoomCacheKeyFn = cacheKeyFn<livekit.Room, WebRoom>()
    private val KeyProviderCacheKeyFn = cacheKeyFn<livekit.BaseKeyProvider, KeyProvider>()
    private val LocalParticipantCacheKeyFn =
        cacheKeyFn<livekit.LocalParticipant, WebLocalParticipant>()

    private val RemoteParticipantCacheKeyFn =
        cacheKeyFn<livekit.RemoteParticipant, WebRemoteParticipant>()
    private val LocalTrackPublicationCacheKeyFn =
        cacheKeyFn<livekit.LocalTrackPublication, WebLocalTrackPublication>()
    private val RemoteTrackPublicationCacheKeyFn =
        cacheKeyFn<livekit.RemoteTrackPublication, WebRemoteTrackPublication>()

    private val LocalAudioTrackCacheKeyFn =
        cacheKeyFn<livekit.LocalAudioTrack, WebLocalAudioTrack>()
    private val RemoteAudioTrackCacheKeyFn =
        cacheKeyFn<livekit.RemoteAudioTrack, WebRemoteAudioTrack>()

    private val LocalVideoTrackCacheKeyFn =
        cacheKeyFn<livekit.LocalVideoTrack, WebLocalVideoTrack>()
    private val RemoteVideoTrackCacheKeyFn =
        cacheKeyFn<livekit.RemoteVideoTrack, WebRemoteVideoTrack>()

    private inline fun <K : Any, reified V : Any> cacheKeyFn(): (K) -> CacheKey<V> {
      val clazz = V::class
      return { key ->
        CacheKey(
            identity = key,
            clazz = clazz,
        )
      }
    }

    internal fun toCommon(
        participantPermissions: livekit.ParticipantPermissions?,
    ): ParticipantPermission? {
      return if (participantPermissions == null) null else toCommon(participantPermissions)
    }

    internal fun toCommon(
        participantPermissions: livekit.ParticipantPermissions,
    ): ParticipantPermission {
      return ParticipantPermission(
          canPublish = participantPermissions.canPublish,
          canSubscribe = participantPermissions.canSubscribe,
          canPublishData = participantPermissions.canPublishData,
          hidden = participantPermissions.hidden,
          recorder = participantPermissions.recorder,
          canPublishSources =
              participantPermissions.canPublishSources.asSequence().map(::toCommon).toList(),
          canUpdateMetadata = participantPermissions.canUpdateMetadata,
          canSubscribeMetrics = participantPermissions.canSubscribeMetrics,
      )
    }

    internal fun toCommon(trackDimensions: livekit.TrackDimensions): Track.Dimensions {
      return Track.Dimensions(
          width = trackDimensions.width.toInt(),
          height = trackDimensions.height.toInt(),
      )
    }

    internal fun toCommon(protocolTrackSource: livekit.ProtocolTrackSource): Track.Source {
      return when (protocolTrackSource) {
        livekit.ProtocolTrackSources.UNKNOWN -> Track.Source.UNKNOWN
        livekit.ProtocolTrackSources.CAMERA -> Track.Source.CAMERA
        livekit.ProtocolTrackSources.MICROPHONE -> Track.Source.MICROPHONE
        livekit.ProtocolTrackSources.SCREEN_SHARE -> Track.Source.SCREEN_SHARE
        livekit.ProtocolTrackSources.SCREEN_SHARE_AUDIO -> Track.Source.SCREEN_SHARE_AUDIO
        else -> unreachable()
      }
    }

    internal fun toCommon(permissionStatus: livekit.PermissionStatus): Boolean {
      return when (permissionStatus) {
        livekit.PermissionStatuss.Allowed -> true
        livekit.PermissionStatuss.NotAllowed -> false
        else -> unreachable()
      }
    }

    internal fun toCommon(connectionState: livekit.ConnectionState): Room.State {
      return when (connectionState) {
        livekit.ConnectionStates.Connected -> Room.State.CONNECTED
        livekit.ConnectionStates.Disconnected -> Room.State.DISCONNECTED
        livekit.ConnectionStates.Connecting -> Room.State.CONNECTING
        livekit.ConnectionStates.Reconnecting -> Room.State.RECONNECTING
        livekit.ConnectionStates.SignalReconnecting -> Room.State.RECONNECTING
        else -> unreachable()
      }
    }

    internal fun toCommon(connectionQuality: livekit.ConnectionQuality): ConnectionQuality {
      return when (connectionQuality) {
        livekit.ConnectionQualitys.Excellent -> ConnectionQuality.EXCELLENT
        livekit.ConnectionQualitys.Good -> ConnectionQuality.GOOD
        livekit.ConnectionQualitys.Poor -> ConnectionQuality.POOR
        livekit.ConnectionQualitys.Lost -> ConnectionQuality.LOST
        livekit.ConnectionQualitys.Unknown -> ConnectionQuality.UNKNOWN
        else -> unreachable()
      }
    }

    internal fun toCommon(participantKind: livekit.ParticipantKind): Participant.Kind {
      return when (participantKind) {
        livekit.ParticipantKinds.AGENT -> Participant.Kind.AGENT
        livekit.ParticipantKinds.STANDARD -> Participant.Kind.STANDARD
        livekit.ParticipantKinds.INGRESS -> Participant.Kind.INGRESS
        livekit.ParticipantKinds.EGRESS -> Participant.Kind.EGRESS
        livekit.ParticipantKinds.SIP -> Participant.Kind.SIP
        livekit.ParticipantKinds.CONNECTOR -> Participant.Kind.CONNECTOR
        livekit.ParticipantKinds.BRIDGE -> Participant.Kind.BRIDGE
        else -> Participant.Kind.UNKNOWN
      }
    }

    internal fun toCommon(trackKind: livekit.TrackKind): Track.Kind {
      return when (trackKind) {
        livekit.TrackKinds.Unknown -> Track.Kind.UNRECOGNIZED
        livekit.TrackKinds.Audio -> Track.Kind.AUDIO
        livekit.TrackKinds.Video -> Track.Kind.VIDEO
        else -> unreachable()
      }
    }

    internal fun toCommon(trackStreamState: livekit.TrackStreamState): Track.StreamState {
      return when (trackStreamState) {
        livekit.TrackStreamStates.Active -> Track.StreamState.ACTIVE
        livekit.TrackStreamStates.Paused -> Track.StreamState.PAUSED
        livekit.TrackStreamStates.Unknown -> Track.StreamState.UNKNOWN
        else -> unreachable()
      }
    }

    internal fun toCommon(trackSource: livekit.TrackSource): Track.Source {
      return when (trackSource) {
        livekit.TrackSources.Unknown -> Track.Source.UNKNOWN
        livekit.TrackSources.Camera -> Track.Source.CAMERA
        livekit.TrackSources.Microphone -> Track.Source.MICROPHONE
        livekit.TrackSources.ScreenShare -> Track.Source.SCREEN_SHARE
        livekit.TrackSources.ScreenShareAudio -> Track.Source.SCREEN_SHARE_AUDIO
        else -> unreachable()
      }
    }

    internal fun toCommon(disconnectReason: livekit.DisconnectReason): DisconnectReason {
      return when (disconnectReason) {
        livekit.DisconnectReasons.UNKNOWN_REASON -> DisconnectReason.`UNKNOWN_REASON`
        livekit.DisconnectReasons.`CLIENT_INITIATED` -> DisconnectReason.`CLIENT_INITIATED`
        livekit.DisconnectReasons.`DUPLICATE_IDENTITY` -> DisconnectReason.`DUPLICATE_IDENTITY`
        livekit.DisconnectReasons.`SERVER_SHUTDOWN` -> DisconnectReason.`SERVER_SHUTDOWN`
        livekit.DisconnectReasons.`PARTICIPANT_REMOVED` -> DisconnectReason.`PARTICIPANT_REMOVED`
        livekit.DisconnectReasons.`ROOM_DELETED` -> DisconnectReason.`ROOM_DELETED`
        livekit.DisconnectReasons.`STATE_MISMATCH` -> DisconnectReason.`STATE_MISMATCH`
        livekit.DisconnectReasons.`JOIN_FAILURE` -> DisconnectReason.`JOIN_FAILURE`
        livekit.DisconnectReasons.`MIGRATION` -> DisconnectReason.`MIGRATION`
        livekit.DisconnectReasons.`SIGNAL_CLOSE` -> DisconnectReason.`SIGNAL_CLOSE`
        livekit.DisconnectReasons.`ROOM_CLOSED` -> DisconnectReason.`ROOM_CLOSED`
        livekit.DisconnectReasons.`USER_UNAVAILABLE` -> DisconnectReason.`USER_UNAVAILABLE`
        livekit.DisconnectReasons.`USER_REJECTED` -> DisconnectReason.`USER_REJECTED`
        livekit.DisconnectReasons.`SIP_TRUNK_FAILURE` -> DisconnectReason.`SIP_TRUNK_FAILURE`
        livekit.DisconnectReasons.`CONNECTION_TIMEOUT` -> DisconnectReason.`CONNECTION_TIMEOUT`
        livekit.DisconnectReasons.`MEDIA_FAILURE` -> DisconnectReason.`MEDIA_FAILURE`
        livekit.DisconnectReasons.`AGENT_ERROR` -> DisconnectReason.UNKNOWN_REASON
        else -> unreachable()
      }
    }

    internal fun toCommon(
        subscriptionStatus: livekit.SubscriptionStatus,
    ): RemoteTrackPublication.SubscriptionStatus {
      return when (subscriptionStatus) {
        livekit.SubscriptionStatuss.Desired ->
            RemoteTrackPublication.SubscriptionStatus.UNSUBSCRIBED

        livekit.SubscriptionStatuss.Subscribed ->
            RemoteTrackPublication.SubscriptionStatus.SUBSCRIBED

        livekit.SubscriptionStatuss.Unsubscribed ->
            RemoteTrackPublication.SubscriptionStatus.UNSUBSCRIBED

        else -> unreachable()
      }
    }

    internal fun toCommon(attributes: JsRecord<JsString>): Map<String, String> {
      return attributes.asSequence().associate { (key, value) ->
        toKotlinString(key) to toKotlinString(value)
      }
    }

    internal fun fromCommon(videoQuality: VideoQuality): livekit.VideoQuality {
      return when (videoQuality) {
        VideoQuality.LOW -> livekit.VideoQualitys.LOW
        VideoQuality.MEDIUM -> livekit.VideoQualitys.MEDIUM
        VideoQuality.HIGH -> livekit.VideoQualitys.HIGH
      }
    }

    internal fun fromCommon(trackDimensions: Track.Dimensions): livekit.TrackDimensions {
      return livekit.TrackDimensions {
        this.height = trackDimensions.height.toDouble()
        this.width = trackDimensions.width.toDouble()
      }
    }

    internal fun fromCommon(connectOptions: ConnectOptions): livekit.RoomConnectOptions {
      return livekit.RoomConnectOptions {
        this.autoSubscribe = connectOptions.autoSubscribe
      }
    }

    internal fun fromCommon(roomOptions: RoomOptions): livekit.RoomOptions {
      return livekit.RoomOptions {
        this.adaptiveStream = AdaptiveStreamChoice(roomOptions.adaptiveStream)
        this.dynacast = roomOptions.dynacast
        this.encryption = fromCommon(roomOptions.e2eeOptions)
        this.reconnectPolicy = fromCommon(roomOptions.reconnectPolicy)
      }
    }

    internal fun fromCommon(
        audioTrackCaptureDefaults: LocalAudioTrackOptions?,
    ): livekit.AudioCaptureOptions? {
      if (audioTrackCaptureDefaults == null) return null

      return livekit.AudioCaptureOptions {
        this.noiseSuppression =
            livekit.BooleanConstraint(audioTrackCaptureDefaults.noiseSuppression)
        this.echoCancellation =
            livekit.BooleanConstraint(audioTrackCaptureDefaults.echoCancellation)
        this.autoGainControl = livekit.BooleanConstraint(audioTrackCaptureDefaults.autoGainControl)
        // TODO(fhilgers): where are they on web
        // val highPassFilter: Boolean = true
        // val typingNoiseDetection: Boolean = true
      }
    }

    internal fun fromCommon(
        videoTrackCaptureDefaults: LocalVideoTrackOptions?,
    ): livekit.VideoCaptureOptions? {
      if (videoTrackCaptureDefaults == null) return null

      return livekit.VideoCaptureOptions {
        // TODO(fhilgers): where are they on web
        // val isScreencast: Boolean = false,
        this.deviceId = videoTrackCaptureDefaults.deviceId?.let(::DeviceConstraint)
        this.facingMode = fromCommon(videoTrackCaptureDefaults.position)
        this.resolution = fromCommon(videoTrackCaptureDefaults.captureParams)
      }
    }

    private fun fromCommon(videoCaptureParameter: VideoCaptureParameter): livekit.VideoResolution {
      return livekit.VideoResolution {
        this.height = videoCaptureParameter.height.toDouble()
        this.width = videoCaptureParameter.width.toDouble()
        // TODO(fhilgers): what to do here?
        // this.aspectRatio
        // videoCaptureParameter.adaptOutputToDimensions
        this.frameRate = videoCaptureParameter.maxFps.toDouble()
      }
    }

    private fun fromCommon(cameraPosition: CameraPosition?): livekit.FacingMode? {
      return if (cameraPosition == null) null else fromCommon(cameraPosition)
    }

    private fun fromCommon(cameraPosition: CameraPosition): livekit.FacingMode {
      return when (cameraPosition) {
        CameraPosition.FRONT -> livekit.FacingModes.User
        CameraPosition.BACK -> livekit.FacingModes.Environment
      }
    }

    private fun fromCommon(e2EEOptions: E2EEOptions?): livekit.E2EEOptions? {
      return if (e2EEOptions == null) null else fromCommon(e2EEOptions)
    }

    private fun fromCommon(e2EEOptions: E2EEOptions): livekit.E2EEOptions {
      return livekit.E2EEOptions(
          livekit.E2EEManagerOptions {
            this.keyProvider =
                NativeBaseKeyProvider(
                    options = fromCommon(e2EEOptions.keyProviderOptions),
                    algorithm = e2EEOptions.keyProviderOptions.keyDerivationAlgorithm,
                )
            this.worker = makeLivekitWorker()
          },
      )
    }

    private fun fromCommon(
        keyProviderOptions: KeyProviderOptions,
    ): livekit.PartialKeyProviderOptions {
      return livekit.PartialKeyProviderOptions {
        this.sharedKey = keyProviderOptions.enableSharedKey
        this.ratchetSalt = keyProviderOptions.ratchetSalt
        this.ratchetWindowSize = keyProviderOptions.ratchetWindowSize.toDouble()
        this.failureTolerance = keyProviderOptions.failureTolerance.toDouble()
        this.keyringSize = keyProviderOptions.keyRingSize.toDouble()
      }
    }

    internal fun toEpochMillis(value: livekit.Date): Long? {
      val raw = getTimeImpl(value)
      return if (raw.isNaN()) null else raw.toLong()
    }

    internal fun toEpochMillis(value: livekit.Date?): Long? {
      return if (value == null) null else toEpochMillis(value)
    }

    private fun fromCommon(reconnectPolicy: ReconnectPolicy?): livekit.ReconnectPolicy? {
      return if (reconnectPolicy == null) null else fromCommon(reconnectPolicy)
    }

    private fun fromCommon(reconnectPolicy: ReconnectPolicy): livekit.ReconnectPolicy {
      return livekit.ReconnectPolicy {
        nextRetryDelayInMs = nextRetryDelayInMs@{ context ->
          val nextRetryDelay =
              reconnectPolicy.getNextRetryDelay(
                  context = toCommon(context),
              ) ?: return@nextRetryDelayInMs null

          nextRetryDelay.inWholeMilliseconds.toDouble()
        }
      }
    }

    private fun toCommon(reconnectContext: livekit.ReconnectContext): ReconnectContext {
      return ReconnectContext(
          retryCount = reconnectContext.retryCount.toInt(),
          elapsedTime = reconnectContext.elapsedMs.milliseconds,
      )
    }

    internal fun toParticipantIdentity(identity: String): Participant.Identity {
      return Participant.Identity(identity)
    }

    internal fun toParticipantSid(sid: String): Participant.Sid {
      return Participant.Sid(sid)
    }

    internal fun toRoomSid(sid: JsString): Room.Sid {
      return Room.Sid(toKotlinString(sid))
    }

    internal fun toRoomSid(roomModel: RoomModel?): Room.Sid? {
      return if (roomModel == null) null else Room.Sid(roomModel.sid)
    }

    internal fun toCameraPosition(cameraPosition: String): CameraPosition? {
      return when (cameraPosition) {
        "user" -> CameraPosition.FRONT
        "environment" -> CameraPosition.BACK
        "left" -> CameraPosition.FRONT
        "right" -> CameraPosition.FRONT
        else -> null
      }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    internal suspend fun <T : JsAny?> await(promise: Promise<T>): T {
      return suspendCancellableCoroutine { continuation ->
        promise
            .then {
              if (continuation.isActive) {
                continuation.resume(it)
              }
              null
            }
            .catch { error ->
              if (continuation.isActive) {
                continuation.resumeWithException(
                    error.asJsException(),
                )
              }
              null
            }
      }
    }

    internal fun <V : JsAny?> asSequence(record: JsArray<V>): Sequence<V> {
      return record.asSequence()
    }

    internal fun <V : JsAny?> asSequence(record: JsRecord<V>): Sequence<Pair<JsString, V>> {
      return record.asSequence()
    }

    internal fun toKotlinString(value: JsString): String {
      return jsToKotlinString(value)
    }

    internal fun unreachable(): Nothing {
      error("unreachable")
    }
  }
}

private fun childSupervisorScope(parentScope: CoroutineScope): CoroutineScope {
  return CoroutineScope(
      parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext.job),
  )
}

private fun jsToKotlinString(@Suppress("unused") value: JsString): String = js("""value""")

@Suppress("UNCHECKED_CAST")
private fun <T : JsAny?> JsAny?.castNullable(): T {
  if (this == null) return null as T
  // T & Any satisfies the non-nullable upper bound required by unsafeCast<T>()
  return this.unsafeCast<T & Any>() as T
}

// 1. External interfaces extending JsAny
private external interface JsIteratorResult<out T : JsAny?> : JsAny {
  val value: T
  val done: Boolean
}

private external interface JsIterator<out T : JsAny?> : JsAny {
  fun next(): JsIteratorResult<T>
}

// Map entries are JS 2-element arrays: [key, value]
private external interface JsEntry<out K : JsAny?, out V : JsAny?> : JsAny

// Dynamic indexed read on the tuple
@Suppress("UNUSED_PARAMETER")
private fun jsEntryGet(entry: JsAny, index: Int): JsAny = js("entry[index]")

// Tuple accessors using castNullable()
private inline val <K : JsAny?, V : JsAny?> JsEntry<K, V>.key: K
  get() = jsEntryGet(this, 0).castNullable<K>()

private inline val <K : JsAny?, V : JsAny?> JsEntry<K, V>.value: V
  get() = jsEntryGet(this, 1).castNullable<V>()

// 2. Native ES6 Map external class
@JsName("Map")
private external class CapableJsMap<K : JsAny?, V : JsAny?> : JsAny {
  val size: Int

  fun entries(): JsIterator<JsEntry<K, V>>

  fun keys(): JsIterator<K>

  fun values(): JsIterator<V>
}

@JsName("Map")
private external class CapableJsArray<V : JsAny?> : JsAny {
  fun values(): JsIterator<V>
}

private fun <T : JsAny?> JsIterator<T>.asKotlinIterator(): Iterator<T> {
  val jsNext = ::next
  return object : Iterator<T> {
    private var step: JsIteratorResult<T> = jsNext()

    override fun hasNext(): Boolean = !step.done

    override fun next(): T {
      if (step.done) throw NoSuchElementException("Iterator has reached the end.")
      val current: T = step.value
      step = jsNext()
      return current
    }
  }
}

// 4. Wrap to Kotlin Sequence
private fun <K : JsAny?, V : JsAny?> CapableJsMap<K, V>.asSequence(): Sequence<Pair<K, V>> {
  return this.entries().asKotlinIterator().asSequence().map { entry -> entry.key to entry.value }
}

internal fun <K : JsAny?, V : JsAny?> livekit.JsMap<K, V>.asSequence(): Sequence<Pair<K, V>> {
  return this.unsafeCast<CapableJsMap<K, V>>().asSequence()
}

private fun <V : JsAny?> CapableJsArray<V>.asSequence(): Sequence<V> {
  return this.values().asKotlinIterator().asSequence()
}

private fun <V : JsAny?> JsArray<V>.asSequence(): Sequence<V> {
  return this.unsafeCast<CapableJsArray<V>>().asSequence()
}

private fun <V : JsAny?> JsRecord<V>.asSequence(): Sequence<Pair<JsString, V>> {
  return objectEntries(this).asKotlinIterator().asSequence().map { entry ->
    entry.key to entry.value
  }
}

private fun <V : JsAny?> objectEntries(
    record: JsRecord<V>,
): JsIterator<JsEntry<JsString, V>> = js("""Object.entries(record).values()""")

private fun getTimeImpl(date: livekit.Date): Double = js("""date.getTime()""")

private fun makeLivekitWorker(): Worker =
    js("""new Worker(new URL('livekit-client/e2ee-worker', import.meta.url))""")
