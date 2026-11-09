package de.connect2x.livekitnity.core.platform

import livekit.ParticipantEvent
import livekit.RoomEvent
import livekit.TrackPublicationEvent

internal object StateDescriptors {

  object Track {
    val streamState =
        StateDescriptor(
            events = listOf(CustomEvent.StreamStateChanged),
            getState = livekit.Track::streamState,
            mapState = Converter::toCommon,
        )
  }

  object TrackPublication {
    val track =
        StateDescriptor(
            events = listOf(CustomEvent.TrackChanged),
            getState = livekit.TrackPublication::track,
            mapState = Converter::toPartialTrack,
        )
    val muted =
        StateDescriptor(
            events =
                listOf(
                    TrackPublicationEvent.Muted,
                    TrackPublicationEvent.Unmuted,
                ),
            getState = livekit.TrackPublication::isMuted,
        )
  }

  object Participant {
    val identity =
        StateDescriptor(
            events = listOf(CustomEvent.IdentityChanged),
            getState = livekit.Participant::identity,
            mapState = Converter::toParticipantIdentity,
        )
    val audioLevel =
        StateDescriptor(
            events = listOf(CustomEvent.AudioLevelChanged),
            getState = livekit.Participant::audioLevel,
            mapState = Double::toFloat,
        )
    val isSpeaking =
        StateDescriptor(
            events = listOf(ParticipantEvent.IsSpeakingChanged),
            getState = livekit.Participant::isSpeaking,
        )
    val name =
        StateDescriptor(
            events = listOf(ParticipantEvent.ParticipantNameChanged),
            getState = livekit.Participant::name,
        )
    val metadata =
        StateDescriptor(
            events = listOf(ParticipantEvent.ParticipantMetadataChanged),
            getState = livekit.Participant::metadata,
        )
    val attributes =
        StateDescriptor(
            events = listOf(ParticipantEvent.AttributesChanged),
            getState = livekit.Participant::attributes,
            mapState = Converter::toCommon,
        )
    val permissions =
        StateDescriptor(
            events = listOf(ParticipantEvent.ParticipantPermissionsChanged),
            getState = livekit.Participant::permissions,
            mapState = Converter::toCommon,
        )
    val connectionQuality =
        StateDescriptor(
            events = listOf(ParticipantEvent.ConnectionQualityChanged),
            getState = livekit.Participant::connectionQuality,
            mapState = Converter::toCommon,
        )
    val lastSpokeAt =
        StateDescriptor(
            events = listOf(ParticipantEvent.IsSpeakingChanged),
            getState = livekit.Participant::lastSpokeAt,
            mapState = Converter::toEpochMillis,
        )
    val kind =
        StateDescriptor(
            // TODO(fhilgers): This never updates, maybe it should just be a normal value
            events = listOf(),
            getState = livekit.Participant::kind,
            mapState = Converter::toCommon,
        )
    val trackPublications =
        StateDescriptor(
            events =
                listOf(
                    ParticipantEvent.LocalTrackPublished,
                    ParticipantEvent.LocalTrackUnpublished,
                    ParticipantEvent.TrackPublished,
                    ParticipantEvent.TrackUnpublished,
                ),
            getState = livekit.Participant::trackPublications,
            mapState = Converter::toCommon,
        )
    val isMicrophoneEnabled =
        StateDescriptor(
            events =
                listOf(
                    ParticipantEvent.TrackMuted,
                    ParticipantEvent.TrackUnmuted,
                    ParticipantEvent.LocalTrackPublished,
                    ParticipantEvent.LocalTrackUnpublished,
                    ParticipantEvent.TrackPublished,
                    ParticipantEvent.TrackUnpublished,
                ),
            getState = livekit.Participant::isMicrophoneEnabled,
        )
    val isCameraEnabled =
        StateDescriptor(
            events =
                listOf(
                    ParticipantEvent.TrackMuted,
                    ParticipantEvent.TrackUnmuted,
                    ParticipantEvent.LocalTrackPublished,
                    ParticipantEvent.LocalTrackUnpublished,
                    ParticipantEvent.TrackPublished,
                    ParticipantEvent.TrackUnpublished,
                ),
            getState = livekit.Participant::isCameraEnabled,
        )
    val isScreenShareEnabled =
        StateDescriptor(
            events =
                listOf(
                    ParticipantEvent.TrackMuted,
                    ParticipantEvent.TrackUnmuted,
                    ParticipantEvent.LocalTrackPublished,
                    ParticipantEvent.LocalTrackUnpublished,
                    ParticipantEvent.TrackPublished,
                    ParticipantEvent.TrackUnpublished,
                ),
            getState = livekit.Participant::isScreenShareEnabled,
        )
  }

  object Room {
    val sid =
        StateDescriptor(
            events =
                listOf(
                    RoomEvent.SignalConnected,
                    RoomEvent.Reconnected,
                    RoomEvent.Disconnected,
                ),
            getState = livekit.Room::roomInfo,
            mapState = Converter::toRoomSid,
        )
    val name =
        StateDescriptor(
            events =
                listOf(
                    RoomEvent.SignalConnected,
                    RoomEvent.Reconnected,
                ),
            getState = livekit.Room::name,
        )
    val state =
        StateDescriptor(
            events =
                listOf(
                    RoomEvent.ConnectionStateChanged,
                ),
            getState = livekit.Room::state,
            mapState = Converter::toCommon,
        )
    val metadata =
        StateDescriptor(
            events =
                listOf(
                    RoomEvent.RoomMetadataChanged,
                    RoomEvent.SignalConnected,
                    RoomEvent.Reconnected,
                ),
            getState = livekit.Room::metadata,
        )
    val isRecording =
        StateDescriptor(
            events =
                listOf(
                    RoomEvent.RecordingStatusChanged,
                    RoomEvent.SignalConnected,
                    RoomEvent.Reconnected,
                ),
            getState = livekit.Room::isRecording,
        )
    val remoteParticipants =
        StateDescriptor(
            events =
                listOf(
                    RoomEvent.ParticipantConnected,
                    RoomEvent.ParticipantDisconnected,
                    RoomEvent.SignalConnected,
                    RoomEvent.Reconnected,
                    RoomEvent.Disconnected,
                ),
            getState = livekit.Room::remoteParticipants,
            mapState = Converter::toCommon,
        )
    val activeSpeakers =
        StateDescriptor(
            events =
                listOf(
                    RoomEvent.ActiveSpeakersChanged,
                    RoomEvent.Disconnected,
                ),
            getState = livekit.Room::activeSpeakers,
            mapState = Converter::toCommon,
        )
  }
}
