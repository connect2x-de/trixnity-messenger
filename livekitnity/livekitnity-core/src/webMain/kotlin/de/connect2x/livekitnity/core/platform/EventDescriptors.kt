package de.connect2x.livekitnity.core.platform

internal object EventDescriptors {
  object Room {
    val connected =
        EventDescriptor(event = livekit.RoomEvent.Connected, mapEvent = RoomCtx::connected)

    val reconnecting =
        EventDescriptor(event = livekit.RoomEvent.Reconnecting, mapEvent = RoomCtx::reconnecting)

    val reconnected =
        EventDescriptor(event = livekit.RoomEvent.Reconnected, mapEvent = RoomCtx::reconnected)

    val disconnected =
        EventDescriptor(event = livekit.RoomEvent.Disconnected, mapEvent = RoomCtx::disconnected)

    val participantConnected =
        EventDescriptor(
            event = livekit.RoomEvent.ParticipantConnected,
            mapEvent = RoomCtx::participantConnected,
        )

    val participantDisconnected =
        EventDescriptor(
            event = livekit.RoomEvent.ParticipantDisconnected,
            mapEvent = RoomCtx::participantDisconnected,
        )

    val activeSpeakersChanged =
        EventDescriptor(
            event = livekit.RoomEvent.ActiveSpeakersChanged,
            mapEvent = RoomCtx::activeSpeakersChanged,
        )

    val roomMetadataChanged =
        EventDescriptor(
            event = livekit.RoomEvent.RoomMetadataChanged,
            mapEvent = RoomCtx::roomMetadataChanged,
        )

    val participantMetadataChanged =
        EventDescriptor(
            event = livekit.RoomEvent.ParticipantMetadataChanged,
            mapEvent = RoomCtx::participantMetadataChanged,
        )

    val participantAttributesChanged =
        EventDescriptor(
            event = livekit.RoomEvent.ParticipantAttributesChanged,
            mapEvent = RoomCtx::participantAttributesChanged,
        )

    val participantNameChanged =
        EventDescriptor(
            event = livekit.RoomEvent.ParticipantNameChanged,
            mapEvent = RoomCtx::participantNameChanged,
        )

    val trackMuted =
        EventDescriptor(event = livekit.RoomEvent.TrackMuted, mapEvent = RoomCtx::trackMuted)

    val trackUnmuted =
        EventDescriptor(event = livekit.RoomEvent.TrackUnmuted, mapEvent = RoomCtx::trackUnmuted)

    val localTrackSubscribed =
        EventDescriptor(
            event = livekit.RoomEvent.LocalTrackSubscribed,
            mapEvent = RoomCtx::localTrackSubscribed,
        )

    val trackPublished =
        EventDescriptor(
            event = livekit.RoomEvent.TrackPublished,
            mapEvent = RoomCtx::trackPublished,
        )

    val trackUnpublished =
        EventDescriptor(
            event = livekit.RoomEvent.TrackUnpublished,
            mapEvent = RoomCtx::trackUnpublished,
        )

    val trackSubscribed =
        EventDescriptor(
            event = livekit.RoomEvent.TrackSubscribed,
            mapEvent = RoomCtx::trackSubscribed,
        )

    val trackSubscriptionFailed =
        EventDescriptor(
            event = livekit.RoomEvent.TrackSubscriptionFailed,
            mapEvent = RoomCtx::trackSubscriptionFailed,
        )

    val trackUnsubscribed =
        EventDescriptor(
            event = livekit.RoomEvent.TrackUnsubscribed,
            mapEvent = RoomCtx::trackUnsubscribed,
        )

    val trackStreamStateChanged =
        EventDescriptor(
            event = livekit.RoomEvent.TrackStreamStateChanged,
            mapEvent = RoomCtx::trackStreamStateChanged,
        )

    val trackSubscriptionPermissionChanged =
        EventDescriptor(
            event = livekit.RoomEvent.TrackSubscriptionPermissionChanged,
            mapEvent = RoomCtx::trackSubscriptionPermissionChanged,
        )

    val connectionQualityChanged =
        EventDescriptor(
            event = livekit.RoomEvent.ConnectionQualityChanged,
            mapEvent = RoomCtx::connectionQualityChanged,
        )

    val participantPermissionsChanged =
        EventDescriptor(
            event = livekit.RoomEvent.ParticipantPermissionsChanged,
            mapEvent = RoomCtx::participantPermissionsChanged,
        )

    val recordingStatusChanged =
        EventDescriptor(
            event = livekit.RoomEvent.RecordingStatusChanged,
            mapEvent = RoomCtx::recordingStatusChanged,
        )

    val encryptionError =
        EventDescriptor(
            event = livekit.RoomEvent.EncryptionError,
            mapEvent = RoomCtx::encryptionError,
        )

    val participantActive =
        EventDescriptor(
            event = livekit.RoomEvent.ParticipantActive,
            mapEvent = RoomCtx::participantActive,
        )

    val all =
        listOf(
            connected,
            reconnecting,
            reconnected,
            disconnected,
            participantConnected,
            participantDisconnected,
            activeSpeakersChanged,
            roomMetadataChanged,
            participantMetadataChanged,
            participantAttributesChanged,
            participantNameChanged,
            trackMuted,
            trackUnmuted,
            localTrackSubscribed,
            trackPublished,
            trackUnpublished,
            trackSubscribed,
            trackSubscriptionFailed,
            trackUnsubscribed,
            trackStreamStateChanged,
            trackSubscriptionPermissionChanged,
            connectionQualityChanged,
            participantPermissionsChanged,
            recordingStatusChanged,
            encryptionError,
            participantActive,
        )
  }

  object Participant {
    val metadataChanged =
        EventDescriptor(
            event = livekit.ParticipantEvent.ParticipantMetadataChanged,
            mapEvent = ParticipantCtx::metadataChanged,
        )

    val nameChanged =
        EventDescriptor(
            event = livekit.ParticipantEvent.ParticipantNameChanged,
            mapEvent = ParticipantCtx::nameChanged,
        )

    val attributesChanged =
        EventDescriptor(
            event = livekit.ParticipantEvent.AttributesChanged,
            mapEvent = ParticipantCtx::attributesChanged,
        )

    val speakingChanged =
        EventDescriptor(
            event = livekit.ParticipantEvent.IsSpeakingChanged,
            mapEvent = ParticipantCtx::speakingChanged,
        )

    val trackMuted =
        EventDescriptor(
            event = livekit.ParticipantEvent.TrackMuted,
            mapEvent = ParticipantCtx::trackMuted,
        )

    val trackUnmuted =
        EventDescriptor(
            event = livekit.ParticipantEvent.TrackUnmuted,
            mapEvent = ParticipantCtx::trackUnmuted,
        )

    val localTrackSubscribed =
        EventDescriptor(
            event = livekit.ParticipantEvent.LocalTrackSubscribed,
            mapEvent = ParticipantCtx::localTrackSubscribed,
        )

    val localTrackPublished =
        EventDescriptor(
            event = livekit.ParticipantEvent.LocalTrackPublished,
            mapEvent = ParticipantCtx::localTrackPublished,
        )

    val localTrackUnpublished =
        EventDescriptor(
            event = livekit.ParticipantEvent.LocalTrackUnpublished,
            mapEvent = ParticipantCtx::localTrackUnpublished,
        )

    val trackPublished =
        EventDescriptor(
            event = livekit.ParticipantEvent.TrackPublished,
            mapEvent = ParticipantCtx::trackPublished,
        )

    val trackUnpublished =
        EventDescriptor(
            event = livekit.ParticipantEvent.TrackUnpublished,
            mapEvent = ParticipantCtx::trackUnpublished,
        )

    val trackSubscribed =
        EventDescriptor(
            event = livekit.ParticipantEvent.TrackSubscribed,
            mapEvent = ParticipantCtx::trackSubscribed,
        )

    val trackSubscriptionFailed =
        EventDescriptor(
            event = livekit.ParticipantEvent.TrackSubscriptionFailed,
            mapEvent = ParticipantCtx::trackSubscriptionFailed,
        )

    val trackUnsubscribed =
        EventDescriptor(
            event = livekit.ParticipantEvent.TrackUnsubscribed,
            mapEvent = ParticipantCtx::trackUnsubscribed,
        )

    val trackStreamStateChanged =
        EventDescriptor(
            event = livekit.ParticipantEvent.TrackStreamStateChanged,
            mapEvent = ParticipantCtx::trackStreamStateChanged,
        )

    val trackSubscriptionPermissionChanged =
        EventDescriptor(
            event = livekit.ParticipantEvent.TrackSubscriptionPermissionChanged,
            mapEvent = ParticipantCtx::trackSubscriptionPermissionChanged,
        )

    val permissionsChanged =
        EventDescriptor(
            event = livekit.ParticipantEvent.ParticipantPermissionsChanged,
            mapEvent = ParticipantCtx::permissionsChanged,
        )

    val all =
        listOf(
            metadataChanged,
            nameChanged,
            attributesChanged,
            speakingChanged,
            trackMuted,
            trackUnmuted,
            localTrackSubscribed,
            localTrackPublished,
            localTrackUnpublished,
            trackPublished,
            trackUnpublished,
            trackSubscribed,
            trackSubscriptionFailed,
            trackUnsubscribed,
            trackStreamStateChanged,
            trackSubscriptionPermissionChanged,
            permissionsChanged,
        )
  }

  object Track {
    val visibilityChanged =
        EventDescriptor(
            event = livekit.TrackEvent.VisibilityChanged,
            mapEvent = TrackCtx::visibilityChanged,
        )

    val videoDimensionsChanged =
        EventDescriptor(
            event = livekit.TrackEvent.VideoDimensionsChanged,
            mapEvent = TrackCtx::videoDimensionsChanged,
        )

    val all =
        listOf(
            visibilityChanged,
            videoDimensionsChanged,
        )
  }
}
