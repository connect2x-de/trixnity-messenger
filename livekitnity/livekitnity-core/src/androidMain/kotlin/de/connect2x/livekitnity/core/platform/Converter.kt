package de.connect2x.livekitnity.core.platform

import android.content.Context
import de.connect2x.livekitnity.api.CameraEnumerator
import de.connect2x.livekitnity.api.ConnectOptions
import de.connect2x.livekitnity.api.RoomOptions
import de.connect2x.livekitnity.api.audio.AudioDevice
import de.connect2x.livekitnity.api.audio.AudioSwitchHandler
import de.connect2x.livekitnity.api.e2ee.E2EEOptions
import de.connect2x.livekitnity.api.e2ee.E2EEState
import de.connect2x.livekitnity.api.e2ee.EncryptionType
import de.connect2x.livekitnity.api.e2ee.KeyDerivationAlgorithm
import de.connect2x.livekitnity.api.e2ee.KeyProviderOptions
import de.connect2x.livekitnity.api.events.ParticipantEvent
import de.connect2x.livekitnity.api.events.RoomEvent
import de.connect2x.livekitnity.api.events.TrackEvent
import de.connect2x.livekitnity.api.events.TrackPublicationEvent
import de.connect2x.livekitnity.api.network.ReconnectContext
import de.connect2x.livekitnity.api.network.ReconnectPolicy
import de.connect2x.livekitnity.api.room.AndroidRoom
import de.connect2x.livekitnity.api.room.DisconnectReason
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.participant.AndroidLocalParticipant
import de.connect2x.livekitnity.api.room.participant.AndroidParticipant
import de.connect2x.livekitnity.api.room.participant.AndroidRemoteParticipant
import de.connect2x.livekitnity.api.room.participant.AudioTrackPublishDefaults
import de.connect2x.livekitnity.api.room.participant.AudioTrackPublishOptions
import de.connect2x.livekitnity.api.room.participant.BackupVideoCodec
import de.connect2x.livekitnity.api.room.participant.ConnectionQuality
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.ParticipantPermission
import de.connect2x.livekitnity.api.room.participant.ParticipantTrackPermission
import de.connect2x.livekitnity.api.room.participant.TrackPublishOptions
import de.connect2x.livekitnity.api.room.participant.VideoTrackPublishOptions
import de.connect2x.livekitnity.api.room.track.AndroidLocalAudioTrack
import de.connect2x.livekitnity.api.room.track.AndroidLocalTrackPublication
import de.connect2x.livekitnity.api.room.track.AndroidLocalVideoTrack
import de.connect2x.livekitnity.api.room.track.AndroidRemoteAudioTrack
import de.connect2x.livekitnity.api.room.track.AndroidRemoteTrackPublication
import de.connect2x.livekitnity.api.room.track.AndroidRemoteVideoTrack
import de.connect2x.livekitnity.api.room.track.AndroidTrack
import de.connect2x.livekitnity.api.room.track.AndroidTrackPublication
import de.connect2x.livekitnity.api.room.track.CameraPosition
import de.connect2x.livekitnity.api.room.track.LocalAudioTrackOptions
import de.connect2x.livekitnity.api.room.track.LocalVideoTrackOptions
import de.connect2x.livekitnity.api.room.track.RemoteTrackPublication
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.VideoCaptureParameter
import de.connect2x.livekitnity.api.room.track.VideoEncoding
import de.connect2x.livekitnity.api.room.track.VideoQuality
import de.connect2x.livekitnity.api.room.track.VideoSinkVisibility
import de.connect2x.livekitnity.core.audio.AndroidAudioSwitchHandler
import de.connect2x.livekitnity.core.e2ee.NativeKeyProvider
import de.connect2x.livekitnity.core.network.CommonToAndroidReconnectPolicy
import de.connect2x.livekitnity.core.room.AndroidRoom
import de.connect2x.livekitnity.core.room.participant.AndroidLocalParticipant
import de.connect2x.livekitnity.core.room.participant.AndroidRemoteParticipant
import de.connect2x.livekitnity.core.room.track.AndroidLocalAudioTrack
import de.connect2x.livekitnity.core.room.track.AndroidLocalTrackPublication
import de.connect2x.livekitnity.core.room.track.AndroidLocalVideoTrack
import de.connect2x.livekitnity.core.room.track.AndroidRemoteAudioTrack
import de.connect2x.livekitnity.core.room.track.AndroidRemoteTrackPublication
import de.connect2x.livekitnity.core.room.track.AndroidRemoteVideoTrack
import io.livekit.android.annotations.Beta
import io.livekit.android.e2ee.BaseKeyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import livekit.LivekitModels
import livekit.org.webrtc.FrameCryptorKeyDerivationAlgorithm

internal class Converter(private val context: Context) {
  private val cache = WeakIdentityCache()

  fun toCommon(original: io.livekit.android.room.Room): AndroidRoom {
    return cache.getOrCreate(AndroidRoomCacheKeyFn(original)) {
      AndroidRoom(original, this)
    }
  }

  fun toCommon(original: io.livekit.android.room.participant.Participant): AndroidParticipant {
    return when (original) {
      is io.livekit.android.room.participant.LocalParticipant -> toCommon(original)
      is io.livekit.android.room.participant.RemoteParticipant -> toCommon(original)
      else -> unreachable()
    }
  }

  fun toCommon(
      original: io.livekit.android.room.participant.LocalParticipant,
  ): AndroidLocalParticipant {
    return cache.getOrCreate(AndroidLocalParticipantCacheKeyFn(original)) {
      AndroidLocalParticipant(original, this, context)
    }
  }

  fun toCommon(
      original: io.livekit.android.room.participant.RemoteParticipant,
  ): AndroidRemoteParticipant {
    return cache.getOrCreate(AndroidRemoteParticipantCacheKeyFn(original)) {
      AndroidRemoteParticipant(original, this)
    }
  }

  fun toCommon(original: io.livekit.android.room.track.TrackPublication): AndroidTrackPublication {
    return when (original) {
      is io.livekit.android.room.track.LocalTrackPublication -> toCommon(original)
      is io.livekit.android.room.track.RemoteTrackPublication -> toCommon(original)
      else -> unreachable()
    }
  }

  fun toCommon(
      original: io.livekit.android.room.track.LocalTrackPublication,
  ): AndroidLocalTrackPublication {
    return cache.getOrCreate(AndroidLocalTrackPublicationCacheKeyFn(original)) {
      AndroidLocalTrackPublication(original, this)
    }
  }

  fun toCommon(
      original: io.livekit.android.room.track.RemoteTrackPublication,
  ): AndroidRemoteTrackPublication {
    return cache.getOrCreate(AndroidRemoteTrackPublicationCacheKeyFn(original)) {
      AndroidRemoteTrackPublication(original, this)
    }
  }

  fun toCommon(original: io.livekit.android.room.track.Track): AndroidTrack {
    return when (original) {
      is io.livekit.android.room.track.LocalAudioTrack -> toCommon(original)
      is io.livekit.android.room.track.LocalVideoTrack -> toCommon(original)
      is io.livekit.android.room.track.RemoteAudioTrack -> toCommon(original)
      is io.livekit.android.room.track.RemoteVideoTrack -> toCommon(original)
      else -> unreachable()
    }
  }

  fun toCommon(original: io.livekit.android.room.track.LocalAudioTrack): AndroidLocalAudioTrack {
    return cache.getOrCreate(AndroidLocalAudioTrackCacheKeyFn(original)) {
      AndroidLocalAudioTrack(
          delegate = original,
          converter = this,
      )
    }
  }

  fun toCommon(original: io.livekit.android.room.track.LocalVideoTrack): AndroidLocalVideoTrack {
    return cache.getOrCreate(AndroidLocalVideoTrackCacheKeyFn(original)) {
      AndroidLocalVideoTrack(
          delegate = original,
          converter = this,
      )
    }
  }

  fun toCommon(original: io.livekit.android.room.track.RemoteAudioTrack): AndroidRemoteAudioTrack {
    return cache.getOrCreate(AndroidRemoteAudioTrackCacheKeyFn(original)) {
      AndroidRemoteAudioTrack(
          delegate = original,
          converter = this,
      )
    }
  }

  fun toCommon(original: io.livekit.android.room.track.RemoteVideoTrack): AndroidRemoteVideoTrack {
    return cache.getOrCreate(AndroidRemoteVideoTrackCacheKeyFn(original)) {
      AndroidRemoteVideoTrack(
          delegate = original,
          converter = this,
      )
    }
  }

  fun toCommon(original: io.livekit.android.audio.AudioSwitchHandler): AudioSwitchHandler {
    return cache.getOrCreate(AudioSwitchHandlerCacheKeyFn(original)) {
      AndroidAudioSwitchHandler(
          audioSwitchHandler = original,
      )
    }
  }

  fun toCommon(event: io.livekit.android.events.TrackPublicationEvent): TrackPublicationEvent? {
    return when (event) {
      is io.livekit.android.events.TrackPublicationEvent.TranscriptionReceived -> null
    }
  }

  fun toCommon(event: io.livekit.android.events.ParticipantEvent): ParticipantEvent? {
    return when (event) {
      is io.livekit.android.events.ParticipantEvent.MetadataChanged ->
          ParticipantEvent.MetadataChanged(
              participant = toCommon(event.participant),
              prevMetadata = event.prevMetadata,
          )

      is io.livekit.android.events.ParticipantEvent.NameChanged ->
          ParticipantEvent.NameChanged(
              participant = toCommon(event.participant),
              name = event.name,
          )

      is io.livekit.android.events.ParticipantEvent.AttributesChanged ->
          ParticipantEvent.AttributesChanged(
              participant = toCommon(event.participant),
              changedAttributes = event.changedAttributes,
          )

      is io.livekit.android.events.ParticipantEvent.SpeakingChanged ->
          ParticipantEvent.SpeakingChanged(
              participant = toCommon(event.participant),
              isSpeaking = event.isSpeaking,
          )

      is io.livekit.android.events.ParticipantEvent.TrackMuted ->
          ParticipantEvent.TrackMuted(
              participant = toCommon(event.participant),
              publication = toCommon(event.publication),
          )

      is io.livekit.android.events.ParticipantEvent.TrackUnmuted ->
          ParticipantEvent.TrackUnmuted(
              participant = toCommon(event.participant),
              publication = toCommon(event.publication),
          )

      is io.livekit.android.events.ParticipantEvent.LocalTrackSubscribed ->
          ParticipantEvent.LocalTrackSubscribed(
              participant = toCommon(event.participant),
              publication = toCommon(event.publication),
          )

      is io.livekit.android.events.ParticipantEvent.LocalTrackPublished ->
          ParticipantEvent.LocalTrackPublished(
              participant = toCommon(event.participant),
              publication = toCommon(event.publication),
          )

      is io.livekit.android.events.ParticipantEvent.LocalTrackUnpublished ->
          ParticipantEvent.LocalTrackUnpublished(
              participant = toCommon(event.participant),
              publication = toCommon(event.publication),
          )

      is io.livekit.android.events.ParticipantEvent.TrackPublished ->
          ParticipantEvent.TrackPublished(
              participant = toCommon(event.participant),
              publication = toCommon(event.publication),
          )

      is io.livekit.android.events.ParticipantEvent.TrackUnpublished ->
          ParticipantEvent.TrackUnpublished(
              participant = toCommon(event.participant),
              publication = toCommon(event.publication),
          )

      is io.livekit.android.events.ParticipantEvent.TrackSubscribed ->
          ParticipantEvent.TrackSubscribed(
              participant = toCommon(event.participant),
              track = toCommon(event.track),
              publication = toCommon(event.publication),
          )

      is io.livekit.android.events.ParticipantEvent.TrackSubscriptionFailed ->
          ParticipantEvent.TrackSubscriptionFailed(
              participant = toCommon(event.participant),
              sid = event.sid,
              exception = event.exception,
          )

      is io.livekit.android.events.ParticipantEvent.TrackUnsubscribed ->
          ParticipantEvent.TrackUnsubscribed(
              participant = toCommon(event.participant),
              track = toCommon(event.track),
              publication = toCommon(event.publication),
          )

      is io.livekit.android.events.ParticipantEvent.TrackStreamStateChanged ->
          ParticipantEvent.TrackStreamStateChanged(
              participant = toCommon(event.participant),
              trackPublication = toCommon(event.trackPublication),
              streamState = toCommon(event.streamState),
          )

      is io.livekit.android.events.ParticipantEvent.TrackSubscriptionPermissionChanged ->
          ParticipantEvent.TrackSubscriptionPermissionChanged(
              participant = toCommon(event.participant),
              trackPublication = toCommon(event.trackPublication),
              subscriptionAllowed = event.subscriptionAllowed,
          )

      is io.livekit.android.events.ParticipantEvent.ParticipantPermissionsChanged ->
          ParticipantEvent.ParticipantPermissionsChanged(
              participant = toCommon(event.participant),
              newPermissions = event.newPermissions?.let(::toCommon),
              oldPermissions = event.oldPermissions?.let(::toCommon),
          )

      is io.livekit.android.events.ParticipantEvent.DataReceived,
      is io.livekit.android.events.ParticipantEvent.LocalTrackPublicationFailed,
      is io.livekit.android.events.ParticipantEvent.StateChanged,
      is io.livekit.android.events.ParticipantEvent.TranscriptionReceived,
      is io.livekit.android.events.ParticipantEvent.DataTrackPublished,
      is io.livekit.android.events.ParticipantEvent.DataTrackUnpublished -> null
    }
  }

  fun toCommon(event: io.livekit.android.events.TrackEvent): TrackEvent? {
    return when (event) {
      is io.livekit.android.events.TrackEvent.VisibilityChanged ->
          TrackEvent.VisibilityChanged(
              track = toCommon(event.track),
              isVisible = event.isVisible,
          )

      is io.livekit.android.events.TrackEvent.VideoDimensionsChanged ->
          TrackEvent.VideoDimensionsChanged(
              track = toCommon(event.track),
              newDimensions = toCommon(event.newDimensions),
          )

      is io.livekit.android.events.TrackEvent.StreamStateChanged -> null
    }
  }

  @OptIn(Beta::class)
  fun toCommon(event: io.livekit.android.events.RoomEvent): RoomEvent? {
    return when (event) {
      is io.livekit.android.events.RoomEvent.Connected ->
          RoomEvent.Connected(
              room = toCommon(event.room),
          )

      is io.livekit.android.events.RoomEvent.Reconnecting ->
          RoomEvent.Reconnecting(
              room = toCommon(event.room),
          )

      is io.livekit.android.events.RoomEvent.Reconnected ->
          RoomEvent.Reconnected(
              room = toCommon(event.room),
          )

      is io.livekit.android.events.RoomEvent.Disconnected ->
          RoomEvent.Disconnected(
              room = toCommon(event.room),
              error = event.error,
              reason = toCommon(event.reason),
          )

      is io.livekit.android.events.RoomEvent.ParticipantConnected ->
          RoomEvent.ParticipantConnected(
              room = toCommon(event.room),
              participant = toCommon(event.participant),
          )

      is io.livekit.android.events.RoomEvent.ParticipantDisconnected ->
          RoomEvent.ParticipantDisconnected(
              room = toCommon(event.room),
              participant = toCommon(event.participant),
          )

      is io.livekit.android.events.RoomEvent.ActiveSpeakersChanged ->
          RoomEvent.ActiveSpeakersChanged(
              room = toCommon(event.room),
              speakers = toCommon(event.speakers),
          )

      is io.livekit.android.events.RoomEvent.RoomMetadataChanged ->
          RoomEvent.RoomMetadataChanged(
              room = toCommon(event.room),
              newMetadata = event.newMetadata,
          )

      is io.livekit.android.events.RoomEvent.ParticipantMetadataChanged ->
          RoomEvent.ParticipantMetadataChanged(
              room = toCommon(event.room),
              participant = toCommon(event.participant),
              prevMetadata = event.prevMetadata,
          )

      is io.livekit.android.events.RoomEvent.ParticipantAttributesChanged ->
          RoomEvent.ParticipantAttributesChanged(
              room = toCommon(event.room),
              participant = toCommon(event.participant),
              changedAttributes = event.changedAttributes,
          )

      is io.livekit.android.events.RoomEvent.ParticipantNameChanged ->
          RoomEvent.ParticipantNameChanged(
              room = toCommon(event.room),
              participant = toCommon(event.participant),
              name = event.name,
          )

      is io.livekit.android.events.RoomEvent.TrackMuted ->
          RoomEvent.TrackMuted(
              room = toCommon(event.room),
              publication = toCommon(event.publication),
              participant = toCommon(event.participant),
          )

      is io.livekit.android.events.RoomEvent.TrackUnmuted ->
          RoomEvent.TrackUnmuted(
              room = toCommon(event.room),
              publication = toCommon(event.publication),
              participant = toCommon(event.participant),
          )

      is io.livekit.android.events.RoomEvent.LocalTrackSubscribed ->
          RoomEvent.LocalTrackSubscribed(
              room = toCommon(event.room),
              publication = toCommon(event.publication),
              participant = toCommon(event.participant),
          )

      is io.livekit.android.events.RoomEvent.TrackPublished ->
          RoomEvent.TrackPublished(
              room = toCommon(event.room),
              publication = toCommon(event.publication),
              participant = toCommon(event.participant),
          )

      is io.livekit.android.events.RoomEvent.TrackUnpublished ->
          RoomEvent.TrackUnpublished(
              room = toCommon(event.room),
              publication = toCommon(event.publication),
              participant = toCommon(event.participant),
          )

      is io.livekit.android.events.RoomEvent.TrackSubscribed ->
          RoomEvent.TrackSubscribed(
              room = toCommon(event.room),
              track = toCommon(event.track),
              publication = toCommon(event.publication),
              participant = toCommon(event.participant),
          )

      is io.livekit.android.events.RoomEvent.TrackSubscriptionFailed ->
          RoomEvent.TrackSubscriptionFailed(
              room = toCommon(event.room),
              sid = event.sid,
              exception = event.exception,
              participant = toCommon(event.participant),
          )

      is io.livekit.android.events.RoomEvent.TrackUnsubscribed ->
          RoomEvent.TrackUnsubscribed(
              room = toCommon(event.room),
              track = toCommon(event.track),
              publications = toCommon(event.publications),
              participant = toCommon(event.participant),
          )

      is io.livekit.android.events.RoomEvent.TrackStreamStateChanged ->
          RoomEvent.TrackStreamStateChanged(
              room = toCommon(event.room),
              trackPublication = toCommon(event.trackPublication),
              streamState = toCommon(event.streamState),
          )

      is io.livekit.android.events.RoomEvent.TrackSubscriptionPermissionChanged ->
          RoomEvent.TrackSubscriptionPermissionChanged(
              room = toCommon(event.room),
              participant = toCommon(event.participant),
              trackPublication = toCommon(event.trackPublication),
              subscriptionAllowed = event.subscriptionAllowed,
          )

      is io.livekit.android.events.RoomEvent.ConnectionQualityChanged ->
          RoomEvent.ConnectionQualityChanged(
              room = toCommon(event.room),
              participant = toCommon(event.participant),
              quality = toCommon(event.quality),
          )

      is io.livekit.android.events.RoomEvent.ParticipantPermissionsChanged ->
          RoomEvent.ParticipantPermissionsChanged(
              room = toCommon(event.room),
              participant = toCommon(event.participant),
              oldPermissions = event.oldPermissions?.let(Converter::toCommon),
          )

      is io.livekit.android.events.RoomEvent.RecordingStatusChanged ->
          RoomEvent.RecordingStatusChanged(
              room = toCommon(event.room),
              isRecording = event.room.isRecording,
          )

      is io.livekit.android.events.RoomEvent.TrackE2EEStateEvent -> {
        val error =
            when (event.state) {
              io.livekit.android.e2ee.E2EEState.NEW,
              io.livekit.android.e2ee.E2EEState.OK,
              io.livekit.android.e2ee.E2EEState.KEY_RATCHETED,
              io.livekit.android.e2ee.E2EEState.MISSING_KEY -> null
              // TODO(fhilgers): Better exception types
              io.livekit.android.e2ee.E2EEState.ENCRYPTION_FAILED ->
                  IllegalStateException("EncryptionError: ENCRYPTION_FAILED")

              io.livekit.android.e2ee.E2EEState.DECRYPTION_FAILED ->
                  IllegalStateException("EncryptionError: DECRYPTION_FAILED")

              io.livekit.android.e2ee.E2EEState.INTERNAL_ERROR ->
                  IllegalStateException("EncryptionError: INTERNAL_ERROR")
            }
        if (error == null) {
          null
        } else {
          RoomEvent.EncryptionError(
              room = toCommon(event.room),
              error = error,
              participant = toCommon(event.participant),
          )
        }
      }

      is io.livekit.android.events.RoomEvent.ParticipantStateChanged ->
          when (event.newState) {
            io.livekit.android.room.participant.Participant.State.JOINING,
            io.livekit.android.room.participant.Participant.State.JOINED,
            io.livekit.android.room.participant.Participant.State.DISCONNECTED,
            io.livekit.android.room.participant.Participant.State.UNKNOWN -> null

            io.livekit.android.room.participant.Participant.State.ACTIVE ->
                RoomEvent.ParticipantActive(
                    room = toCommon(event.room),
                    participant = toCommon(event.participant),
                )
          }

      is io.livekit.android.events.RoomEvent.DataReceived,
      is io.livekit.android.events.RoomEvent.FailedToConnect,
      is io.livekit.android.events.RoomEvent.TrackPublicationFailed,
      is io.livekit.android.events.RoomEvent.TranscriptionReceived,
      is io.livekit.android.events.RoomEvent.DataTrackPublished,
      is io.livekit.android.events.RoomEvent.DataTrackUnpublished -> null
    }
  }

  @JvmName("participantListToCommon")
  fun toCommon(
      original: List<io.livekit.android.room.participant.Participant>,
  ): List<AndroidParticipant> {
    return original.map { toCommon(it) }
  }

  @JvmName("trackPublicationTrackPairToCommon")
  fun toCommon(
      original:
          List<
              Pair<
                  io.livekit.android.room.track.TrackPublication,
                  io.livekit.android.room.track.Track?,
              >,
          >,
  ): List<Pair<AndroidTrackPublication, AndroidTrack?>> {
    return original.map { toCommon(it.first) to it.second?.let { toCommon(it) } }
  }

  @JvmName("participantIdentityMapToCommon")
  fun toCommon(
      original:
          Map<
              io.livekit.android.room.participant.Participant.Identity,
              io.livekit.android.room.participant.RemoteParticipant,
          >,
  ): Map<Participant.Identity, AndroidRemoteParticipant> {
    return original.map { (key, value) -> toCommon(key) to toCommon(value) }.toMap()
  }

  @JvmName("trackPublicationMapToCommon")
  fun toCommon(
      original: Map<String, io.livekit.android.room.track.TrackPublication>,
  ): Map<String, AndroidTrackPublication> {
    return original.mapValues { toCommon(it.value) }
  }

  fun clearCache() {
    cache.clear()
  }

  companion object {
    private val AndroidRoomCacheKeyFn = cacheKeyFn<io.livekit.android.room.Room, AndroidRoom>()
    private val AndroidLocalParticipantCacheKeyFn =
        cacheKeyFn<io.livekit.android.room.participant.LocalParticipant, AndroidLocalParticipant>()
    private val AndroidRemoteParticipantCacheKeyFn =
        cacheKeyFn<
            io.livekit.android.room.participant.RemoteParticipant,
            AndroidRemoteParticipant,
        >()
    private val AndroidLocalTrackPublicationCacheKeyFn =
        cacheKeyFn<
            io.livekit.android.room.track.LocalTrackPublication,
            AndroidLocalTrackPublication,
        >()
    private val AndroidRemoteTrackPublicationCacheKeyFn =
        cacheKeyFn<
            io.livekit.android.room.track.RemoteTrackPublication,
            AndroidRemoteTrackPublication,
        >()
    private val AndroidLocalAudioTrackCacheKeyFn =
        cacheKeyFn<io.livekit.android.room.track.LocalAudioTrack, AndroidLocalAudioTrack>()
    private val AndroidRemoteAudioTrackCacheKeyFn =
        cacheKeyFn<io.livekit.android.room.track.RemoteAudioTrack, AndroidRemoteAudioTrack>()
    private val AndroidLocalVideoTrackCacheKeyFn =
        cacheKeyFn<io.livekit.android.room.track.LocalVideoTrack, AndroidLocalVideoTrack>()
    private val AndroidRemoteVideoTrackCacheKeyFn =
        cacheKeyFn<io.livekit.android.room.track.RemoteVideoTrack, AndroidRemoteVideoTrack>()
    private val AudioSwitchHandlerCacheKeyFn =
        cacheKeyFn<io.livekit.android.audio.AudioSwitchHandler, AudioSwitchHandler>()

    private inline fun <K : Any, reified V : Any> cacheKeyFn(): (K) -> CacheKey<V> {
      val clazz = V::class
      return { identity -> CacheKey(identity, clazz) }
    }

    fun toCommon(original: io.livekit.android.room.network.ReconnectContext): ReconnectContext {
      return ReconnectContext(
          retryCount = original.retryCount,
          elapsedTime = original.elapsedTime,
      )
    }

    fun toCommon(original: io.livekit.android.room.Room.State): Room.State {
      return when (original) {
        io.livekit.android.room.Room.State.CONNECTING -> Room.State.CONNECTING
        io.livekit.android.room.Room.State.CONNECTED -> Room.State.CONNECTED
        io.livekit.android.room.Room.State.DISCONNECTED -> Room.State.DISCONNECTED
        io.livekit.android.room.Room.State.RECONNECTING -> Room.State.RECONNECTING
      }
    }

    fun toCommon(original: io.livekit.android.room.Room.Sid): Room.Sid {
      return Room.Sid(original.sid)
    }

    fun toCommon(original: io.livekit.android.room.participant.Participant.Sid): Participant.Sid {
      return Participant.Sid(original.value)
    }

    fun toCommon(
        original: io.livekit.android.room.participant.Participant.Identity,
    ): Participant.Identity {
      return Participant.Identity(original.value)
    }

    fun toCommon(
        original: io.livekit.android.room.participant.ConnectionQuality,
    ): ConnectionQuality {
      return when (original) {
        io.livekit.android.room.participant.ConnectionQuality.EXCELLENT ->
            ConnectionQuality.EXCELLENT

        io.livekit.android.room.participant.ConnectionQuality.GOOD -> ConnectionQuality.GOOD
        io.livekit.android.room.participant.ConnectionQuality.POOR -> ConnectionQuality.POOR
        io.livekit.android.room.participant.ConnectionQuality.UNKNOWN -> ConnectionQuality.UNKNOWN
        io.livekit.android.room.participant.ConnectionQuality.LOST -> ConnectionQuality.LOST
      }
    }

    fun toCommon(original: io.livekit.android.events.DisconnectReason): DisconnectReason {
      return when (original) {
        io.livekit.android.events.DisconnectReason.UNKNOWN_REASON -> DisconnectReason.UNKNOWN_REASON
        io.livekit.android.events.DisconnectReason.CLIENT_INITIATED ->
            DisconnectReason.CLIENT_INITIATED

        io.livekit.android.events.DisconnectReason.DUPLICATE_IDENTITY ->
            DisconnectReason.DUPLICATE_IDENTITY

        io.livekit.android.events.DisconnectReason.SERVER_SHUTDOWN ->
            DisconnectReason.SERVER_SHUTDOWN

        io.livekit.android.events.DisconnectReason.PARTICIPANT_REMOVED ->
            DisconnectReason.PARTICIPANT_REMOVED

        io.livekit.android.events.DisconnectReason.ROOM_DELETED -> DisconnectReason.ROOM_DELETED
        io.livekit.android.events.DisconnectReason.STATE_MISMATCH -> DisconnectReason.STATE_MISMATCH
        io.livekit.android.events.DisconnectReason.JOIN_FAILURE -> DisconnectReason.JOIN_FAILURE
        io.livekit.android.events.DisconnectReason.MIGRATION -> DisconnectReason.MIGRATION
        io.livekit.android.events.DisconnectReason.SIGNAL_CLOSE -> DisconnectReason.SIGNAL_CLOSE
        io.livekit.android.events.DisconnectReason.ROOM_CLOSED -> DisconnectReason.ROOM_CLOSED
        io.livekit.android.events.DisconnectReason.USER_UNAVAILABLE ->
            DisconnectReason.USER_UNAVAILABLE

        io.livekit.android.events.DisconnectReason.USER_REJECTED -> DisconnectReason.USER_REJECTED
        io.livekit.android.events.DisconnectReason.SIP_TRUNK_FAILURE ->
            DisconnectReason.SIP_TRUNK_FAILURE

        io.livekit.android.events.DisconnectReason.CONNECTION_TIMEOUT ->
            DisconnectReason.CONNECTION_TIMEOUT

        io.livekit.android.events.DisconnectReason.MEDIA_FAILURE -> DisconnectReason.MEDIA_FAILURE
      }
    }

    fun toCommon(
        original: io.livekit.android.room.participant.ParticipantPermission,
    ): ParticipantPermission {
      return ParticipantPermission(
          canPublish = original.canPublish,
          canSubscribe = original.canSubscribe,
          canPublishData = original.canPublishData,
          hidden = original.hidden,
          recorder = original.recorder,
          canPublishSources = toCommon(original.canPublishSources),
          canUpdateMetadata = original.canUpdateMetadata,
          canSubscribeMetrics = original.canSubscribeMetrics,
      )
    }

    fun toCommon(original: io.livekit.android.room.track.Track.Source): Track.Source {
      return when (original) {
        io.livekit.android.room.track.Track.Source.UNKNOWN -> Track.Source.UNKNOWN
        io.livekit.android.room.track.Track.Source.CAMERA -> Track.Source.CAMERA
        io.livekit.android.room.track.Track.Source.MICROPHONE -> Track.Source.MICROPHONE
        io.livekit.android.room.track.Track.Source.SCREEN_SHARE -> Track.Source.SCREEN_SHARE
        io.livekit.android.room.track.Track.Source.SCREEN_SHARE_AUDIO ->
            Track.Source.SCREEN_SHARE_AUDIO
      }
    }

    fun toCommon(original: io.livekit.android.room.participant.Participant.Kind): Participant.Kind {
      return when (original) {
        io.livekit.android.room.participant.Participant.Kind.AGENT -> Participant.Kind.AGENT
        io.livekit.android.room.participant.Participant.Kind.STANDARD -> Participant.Kind.STANDARD
        io.livekit.android.room.participant.Participant.Kind.INGRESS -> Participant.Kind.INGRESS
        io.livekit.android.room.participant.Participant.Kind.EGRESS -> Participant.Kind.EGRESS
        io.livekit.android.room.participant.Participant.Kind.SIP -> Participant.Kind.SIP
        io.livekit.android.room.participant.Participant.Kind.CONNECTOR -> Participant.Kind.CONNECTOR
        io.livekit.android.room.participant.Participant.Kind.BRIDGE -> Participant.Kind.BRIDGE
        io.livekit.android.room.participant.Participant.Kind.UNKNOWN -> Participant.Kind.UNKNOWN
      }
    }

    fun toCommon(original: io.livekit.android.room.track.Track.Kind): Track.Kind {
      return when (original) {
        io.livekit.android.room.track.Track.Kind.AUDIO -> Track.Kind.AUDIO
        io.livekit.android.room.track.Track.Kind.VIDEO -> Track.Kind.VIDEO
        io.livekit.android.room.track.Track.Kind.UNRECOGNIZED -> Track.Kind.UNRECOGNIZED
      }
    }

    fun toCommon(original: io.livekit.android.room.track.Track.Dimensions): Track.Dimensions {
      return Track.Dimensions(
          width = original.width,
          height = original.height,
      )
    }

    fun toCommon(original: io.livekit.android.room.track.Track.StreamState): Track.StreamState {
      return when (original) {
        io.livekit.android.room.track.Track.StreamState.ACTIVE -> Track.StreamState.ACTIVE
        io.livekit.android.room.track.Track.StreamState.PAUSED -> Track.StreamState.PAUSED
        io.livekit.android.room.track.Track.StreamState.UNKNOWN -> Track.StreamState.UNKNOWN
      }
    }

    fun toCommon(
        original: io.livekit.android.room.track.RemoteTrackPublication.SubscriptionStatus,
    ): RemoteTrackPublication.SubscriptionStatus {
      return when (original) {
        io.livekit.android.room.track.RemoteTrackPublication.SubscriptionStatus.SUBSCRIBED ->
            RemoteTrackPublication.SubscriptionStatus.SUBSCRIBED

        io.livekit.android.room.track.RemoteTrackPublication.SubscriptionStatus
            .SUBSCRIBED_AND_NOT_ALLOWED ->
            RemoteTrackPublication.SubscriptionStatus.SUBSCRIBED_AND_NOT_ALLOWED

        io.livekit.android.room.track.RemoteTrackPublication.SubscriptionStatus.UNSUBSCRIBED ->
            RemoteTrackPublication.SubscriptionStatus.UNSUBSCRIBED
      }
    }

    fun toCommon(
        original: io.livekit.android.room.participant.TrackPublishOptions,
    ): TrackPublishOptions {
      return when (original) {
        is io.livekit.android.room.participant.AudioTrackPublishOptions -> toCommon(original)
        is io.livekit.android.room.participant.VideoTrackPublishOptions -> toCommon(original)
        else -> unreachable()
      }
    }

    fun toCommon(original: io.livekit.android.room.track.VideoEncoding): VideoEncoding {
      return VideoEncoding(
          maxBitrate = original.maxBitrate,
          maxFps = original.maxFps,
      )
    }

    fun toCommon(
        original: io.livekit.android.room.track.LocalAudioTrackOptions,
    ): LocalAudioTrackOptions {
      return LocalAudioTrackOptions(
          noiseSuppression = original.noiseSuppression,
          echoCancellation = original.echoCancellation,
          autoGainControl = original.autoGainControl,
          highPassFilter = original.highPassFilter,
          typingNoiseDetection = original.typingNoiseDetection,
      )
    }

    fun toCommon(original: io.livekit.android.room.participant.BackupVideoCodec): BackupVideoCodec {
      return BackupVideoCodec(
          codec = original.codec,
          encoding = original.encoding?.let { toCommon(it) },
          simulcast = original.simulcast,
      )
    }

    fun toCommon(
        original: io.livekit.android.room.participant.VideoTrackPublishOptions,
    ): VideoTrackPublishOptions {
      return VideoTrackPublishOptions(
          name = original.name,
          videoEncoding = original.videoEncoding?.let { toCommon(it) },
          simulcast = original.simulcast,
          videoCodec = original.videoCodec,
          scalabilityMode = original.scalabilityMode,
          backupCodec = original.backupCodec?.let { toCommon(it) },
          source = original.source?.let { toCommon(it) },
          stream = original.stream,
      )
    }

    fun toCommon(
        original: io.livekit.android.room.participant.AudioTrackPublishOptions,
    ): AudioTrackPublishOptions {
      return AudioTrackPublishOptions(
          name = original.name,
          audioBitrate = original.audioBitrate,
          dtx = original.dtx,
          red = original.red,
          source = original.source?.let { toCommon(it) },
          stream = original.stream,
          preconnect = original.preconnect,
      )
    }

    fun toCommon(audioDevice: com.twilio.audioswitch.AudioDevice): AudioDevice {
      return when (audioDevice) {
        is com.twilio.audioswitch.AudioDevice.BluetoothHeadset ->
            AudioDevice.BluetoothHeadset(
                audioDevice.name,
            )

        is com.twilio.audioswitch.AudioDevice.Earpiece -> AudioDevice.Earpiece(audioDevice.name)
        is com.twilio.audioswitch.AudioDevice.Speakerphone ->
            AudioDevice.Speakerphone(audioDevice.name)

        is com.twilio.audioswitch.AudioDevice.WiredHeadset ->
            AudioDevice.WiredHeadset(audioDevice.name)
      }
    }

    fun toCommon(audioDevices: List<com.twilio.audioswitch.AudioDevice>): List<AudioDevice> {
      return audioDevices.map(::toCommon)
    }

    fun toCommon(
        original: io.livekit.android.room.participant.Participant.State,
    ): Participant.State {
      return when (original) {
        io.livekit.android.room.participant.Participant.State.JOINING -> Participant.State.JOINING
        io.livekit.android.room.participant.Participant.State.JOINED -> Participant.State.JOINED
        io.livekit.android.room.participant.Participant.State.ACTIVE -> Participant.State.ACTIVE
        io.livekit.android.room.participant.Participant.State.DISCONNECTED ->
            Participant.State.DISCONNECTED

        io.livekit.android.room.participant.Participant.State.UNKNOWN -> Participant.State.UNKNOWN
      }
    }

    fun toCommon(original: io.livekit.android.e2ee.E2EEState): E2EEState {
      return when (original) {
        io.livekit.android.e2ee.E2EEState.NEW -> E2EEState.NEW
        io.livekit.android.e2ee.E2EEState.OK -> E2EEState.OK
        io.livekit.android.e2ee.E2EEState.KEY_RATCHETED -> E2EEState.KEY_RATCHETED
        io.livekit.android.e2ee.E2EEState.MISSING_KEY -> E2EEState.MISSING_KEY
        io.livekit.android.e2ee.E2EEState.ENCRYPTION_FAILED -> E2EEState.ENCRYPTION_FAILED
        io.livekit.android.e2ee.E2EEState.DECRYPTION_FAILED -> E2EEState.DECRYPTION_FAILED
        io.livekit.android.e2ee.E2EEState.INTERNAL_ERROR -> E2EEState.INTERNAL_ERROR
      }
    }

    fun toCommon(original: livekit.org.webrtc.CameraEnumerator): CameraEnumerator {
      return object : CameraEnumerator {
        override val deviceNames: List<String>
          get() = original.deviceNames.toList()

        override fun isBackFacing(deviceName: String): Boolean {
          return original.isBackFacing(deviceName)
        }

        override fun isFrontFacing(deviceName: String): Boolean {
          return original.isFrontFacing(deviceName)
        }
      }
    }

    fun toCommon(
        original: io.livekit.android.room.track.LocalVideoTrackOptions,
    ): LocalVideoTrackOptions {
      return LocalVideoTrackOptions(
          isScreencast = original.isScreencast,
          deviceId = original.deviceId,
          position = original.position?.let { toCommon(it) },
          captureParams = toCommon(original.captureParams),
      )
    }

    fun toCommon(original: io.livekit.android.room.track.CameraPosition): CameraPosition {
      return when (original) {
        io.livekit.android.room.track.CameraPosition.FRONT -> CameraPosition.FRONT
        io.livekit.android.room.track.CameraPosition.BACK -> CameraPosition.BACK
      }
    }

    fun toCommon(
        original: io.livekit.android.room.track.VideoCaptureParameter,
    ): VideoCaptureParameter {
      return VideoCaptureParameter(
          width = original.width,
          height = original.height,
          maxFps = original.maxFps,
          adaptOutputToDimensions = original.adaptOutputToDimensions,
      )
    }

    @JvmName("trackSourceListToCommon")
    fun toCommon(original: List<io.livekit.android.room.track.Track.Source>): List<Track.Source> {
      return original.map { toCommon(it) }
    }

    fun fromCommon(original: RoomOptions): io.livekit.android.RoomOptions {
      return io.livekit.android.RoomOptions(
          adaptiveStream = original.adaptiveStream,
          dynacast = original.dynacast,
          e2eeOptions = original.e2eeOptions?.let { fromCommon(it) },
          reconnectPolicy = original.reconnectPolicy?.let { fromCommon(it) },
      )
    }

    fun fromCommon(
        original: E2EEOptions,
    ): io.livekit.android.e2ee.E2EEOptions {
      return io.livekit.android.e2ee.E2EEOptions(
          keyProvider = fromCommon(original.keyProviderOptions),
          encryptionType = fromCommon(original.encryptionType),
      )
    }

    fun fromCommon(original: EncryptionType): LivekitModels.Encryption.Type {
      return when (original) {
        EncryptionType.NONE -> LivekitModels.Encryption.Type.NONE
        EncryptionType.GCM -> LivekitModels.Encryption.Type.GCM
        EncryptionType.CUSTOM -> LivekitModels.Encryption.Type.CUSTOM
        EncryptionType.UNRECOGNIZED -> LivekitModels.Encryption.Type.UNRECOGNIZED
      }
    }

    fun fromCommon(keyProviderOptions: KeyProviderOptions): io.livekit.android.e2ee.KeyProvider {
      return NativeKeyProvider(
          BaseKeyProvider(
              ratchetSalt = keyProviderOptions.ratchetSalt,
              uncryptedMagicBytes = "",
              ratchetWindowSize = keyProviderOptions.ratchetWindowSize,
              enableSharedKey = keyProviderOptions.enableSharedKey,
              failureTolerance = keyProviderOptions.failureTolerance,
              keyRingSize = keyProviderOptions.keyRingSize,
              discardFrameWhenCryptorNotReady = true,
              keyDerivationAlgorithm =
                  when (keyProviderOptions.keyDerivationAlgorithm) {
                    KeyDerivationAlgorithm.PBKDF2 -> FrameCryptorKeyDerivationAlgorithm.PBKDF2
                    KeyDerivationAlgorithm.HKDF -> FrameCryptorKeyDerivationAlgorithm.HKDF
                  },
          ),
      )
    }

    fun fromCommon(
        original: LocalAudioTrackOptions,
    ): io.livekit.android.room.track.LocalAudioTrackOptions {
      return io.livekit.android.room.track.LocalAudioTrackOptions(
          noiseSuppression = original.noiseSuppression,
          echoCancellation = original.echoCancellation,
          autoGainControl = original.autoGainControl,
          highPassFilter = original.highPassFilter,
          typingNoiseDetection = original.typingNoiseDetection,
      )
    }

    fun fromCommon(
        original: LocalVideoTrackOptions,
    ): io.livekit.android.room.track.LocalVideoTrackOptions {
      return io.livekit.android.room.track.LocalVideoTrackOptions(
          isScreencast = original.isScreencast,
          deviceId = original.deviceId,
          position = original.position?.let { fromCommon(it) },
          captureParams = fromCommon(original.captureParams),
      )
    }

    fun fromCommon(
        original: AudioTrackPublishOptions,
    ): io.livekit.android.room.participant.AudioTrackPublishOptions {
      return io.livekit.android.room.participant.AudioTrackPublishOptions(
          name = original.name,
          audioBitrate = original.audioBitrate,
          dtx = original.dtx,
          red = original.red,
          source = original.source?.let { fromCommon(it) },
          stream = original.stream,
          preconnect = original.preconnect,
      )
    }

    fun fromCommon(
        original: AudioTrackPublishDefaults,
    ): io.livekit.android.room.participant.AudioTrackPublishDefaults {
      return io.livekit.android.room.participant.AudioTrackPublishDefaults(
          audioBitrate = original.audioBitrate,
          dtx = original.dtx,
          red = original.red,
          preconnect = original.preconnect,
      )
    }

    fun fromCommon(original: CameraPosition): io.livekit.android.room.track.CameraPosition {
      return when (original) {
        CameraPosition.FRONT -> io.livekit.android.room.track.CameraPosition.FRONT
        CameraPosition.BACK -> io.livekit.android.room.track.CameraPosition.BACK
      }
    }

    fun fromCommon(
        original: VideoCaptureParameter,
    ): io.livekit.android.room.track.VideoCaptureParameter {
      return io.livekit.android.room.track.VideoCaptureParameter(
          width = original.width,
          height = original.height,
          maxFps = original.maxFps,
          adaptOutputToDimensions = original.adaptOutputToDimensions,
      )
    }

    fun fromCommon(original: ReconnectPolicy): io.livekit.android.room.network.ReconnectPolicy {
      return CommonToAndroidReconnectPolicy(original)
    }

    fun fromCommon(original: Track.Source): io.livekit.android.room.track.Track.Source {
      return when (original) {
        Track.Source.UNKNOWN -> io.livekit.android.room.track.Track.Source.UNKNOWN
        Track.Source.CAMERA -> io.livekit.android.room.track.Track.Source.CAMERA
        Track.Source.MICROPHONE -> io.livekit.android.room.track.Track.Source.MICROPHONE
        Track.Source.SCREEN_SHARE -> io.livekit.android.room.track.Track.Source.SCREEN_SHARE
        Track.Source.SCREEN_SHARE_AUDIO ->
            io.livekit.android.room.track.Track.Source.SCREEN_SHARE_AUDIO
      }
    }

    fun fromCommon(
        original: Participant.Identity,
    ): io.livekit.android.room.participant.Participant.Identity {
      return io.livekit.android.room.participant.Participant.Identity(
          value = original.value,
      )
    }

    fun fromCommon(original: Participant.Sid): io.livekit.android.room.participant.Participant.Sid {
      return io.livekit.android.room.participant.Participant.Sid(
          value = original.value,
      )
    }

    fun fromCommon(original: ConnectOptions): io.livekit.android.ConnectOptions {
      return io.livekit.android.ConnectOptions(
          autoSubscribe = original.autoSubscribe,
      )
    }

    fun fromCommon(original: Track.Dimensions): io.livekit.android.room.track.Track.Dimensions {
      return io.livekit.android.room.track.Track.Dimensions(
          width = original.width,
          height = original.height,
      )
    }

    fun fromCommon(original: VideoQuality): io.livekit.android.room.track.VideoQuality {
      return when (original) {
        VideoQuality.LOW -> io.livekit.android.room.track.VideoQuality.LOW
        VideoQuality.MEDIUM -> io.livekit.android.room.track.VideoQuality.MEDIUM
        VideoQuality.HIGH -> io.livekit.android.room.track.VideoQuality.HIGH
      }
    }

    fun fromCommon(
        original: ParticipantTrackPermission,
    ): io.livekit.android.room.participant.ParticipantTrackPermission {
      return io.livekit.android.room.participant.ParticipantTrackPermission(
          participantIdentity = original.participantIdentity,
          participantSid = original.participantSid,
          allTracksAllowed = original.allTracksAllowed,
          allowedTrackSids = original.allowedTrackSids,
      )
    }

    fun fromCommon(
        original: VideoTrackPublishOptions,
    ): io.livekit.android.room.participant.VideoTrackPublishOptions {
      return io.livekit.android.room.participant.VideoTrackPublishOptions(
          name = original.name,
          videoEncoding = original.videoEncoding?.let { fromCommon(it) },
          simulcast = original.simulcast,
          videoCodec = original.videoCodec,
          scalabilityMode = original.scalabilityMode,
          backupCodec = original.backupCodec?.let { fromCommon(it) },
          source = original.source?.let { fromCommon(it) },
          stream = original.stream,
          /* degradationPreference = degradationPreference, */
          /* simulcastLayers = simulcastLayers, */
      )
    }

    fun fromCommon(original: VideoEncoding): io.livekit.android.room.track.VideoEncoding {
      return io.livekit.android.room.track.VideoEncoding(
          maxBitrate = original.maxBitrate,
          maxFps = original.maxFps,
      )
    }

    fun fromCommon(
        original: BackupVideoCodec,
    ): io.livekit.android.room.participant.BackupVideoCodec {
      return io.livekit.android.room.participant.BackupVideoCodec(
          codec = original.codec,
          encoding = original.encoding?.let { fromCommon(it) },
          simulcast = original.simulcast,
      )
    }

    fun fromCommon(
        videoSinkVisibility: VideoSinkVisibility,
    ): io.livekit.android.room.track.video.VideoSinkVisibility {
      return object : io.livekit.android.room.track.video.VideoSinkVisibility() {

        private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

        init {
          scope.launch {
            videoSinkVisibility.visibilityState.collectLatest {
              notifyChanged()
            }
          }
        }

        override fun isVisible(): Boolean {
          return videoSinkVisibility.visibilityState.value.isVisible
        }

        override fun size(): io.livekit.android.room.track.Track.Dimensions {
          return fromCommon(videoSinkVisibility.visibilityState.value.size)
        }

        override fun close() {
          super.close()
          scope.cancel()
        }
      }
    }

    private fun unreachable(): Nothing {
      error("unreachable")
    }
  }
}
