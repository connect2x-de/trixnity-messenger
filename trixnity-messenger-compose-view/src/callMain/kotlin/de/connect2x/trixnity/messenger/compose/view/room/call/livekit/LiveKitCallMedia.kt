package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import de.connect2x.livekitnity.api.LiveKit
import de.connect2x.livekitnity.api.RoomOptions
import de.connect2x.livekitnity.api.e2ee.E2EEOptions
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.compose.types.TrackReference
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallConnectionState
import de.connect2x.trixnity.messenger.call.CallMedia
import de.connect2x.trixnity.messenger.call.CallMediaError
import de.connect2x.trixnity.messenger.call.CallMediaState
import de.connect2x.trixnity.messenger.call.CallParticipant
import de.connect2x.trixnity.messenger.call.CallPermissionKind
import de.connect2x.trixnity.messenger.call.CallSignalling
import de.connect2x.trixnity.messenger.call.CallSignalling.LiveKitCallParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(MSC4143::class, ExperimentalCoroutinesApi::class)
@TrixnityMessengerCallApi
internal class LiveKitCallMedia(
    private val scope: CoroutineScope,
    private val liveKit: LiveKit,
    private val rtc: CallSignalling,
    requestPermission: (CallPermissionKind) -> Unit,
) : CallMedia {
    private class RoomConnection(val room: Room, val keyProvider: MatrixRtcKeyProvider?)

    private val log = Logger("de.connect2x.trixnity.messenger.compose.view.room.call.livekit.LiveKitCallMedia")

    private data class Rooms(
        val publishing: RoomConnection,
        val receiving: List<RoomConnection>,
        val localIdentity: String,
        val receivingCreationFailed: Boolean = false,
    )

    private val rooms = MutableStateFlow<Rooms?>(null)
    private val controls = CallMediaControls(requestPermission)
    private val publishingFailed = MutableStateFlow(false)

    init {
        scope.launch {
            try {
                // Keep each server's initial credentials; encryption keys update independently below.
                rtc.publishing
                    .filterNotNull()
                    .distinctUntilChangedBy { it.url to (it.encryptionInfo != null) }
                    .collectLatest { publishing ->
                        coroutineScope {
                            val publisher = createRoom(publishing)
                            val identity = matrixRtcParticipantIdentity(publishing.userId, publishing.memberId)
                            val receivers = mutableMapOf<String, RoomConnection>()
                            try {
                                rooms.value = Rooms(publisher, emptyList(), identity)
                                publishingFailed.value = false
                                launch { connectRoom(publisher, publishing) }
                                launch { bindMedia(publisher.room) }
                                rtc.receiving
                                    .distinctUntilChangedBy { connections ->
                                        connections.map { it.url to (it.encryptionInfo != null) }.toSet()
                                    }
                                    .collect { receiving ->
                                        val connections = receiving.associateBy { it.url }
                                        val removed =
                                            receivers.keys.filter { url ->
                                                val parameters = connections[url]
                                                val encrypted = receivers.getValue(url).keyProvider != null
                                                parameters == null || encrypted != (parameters.encryptionInfo != null)
                                            }
                                        val retired = removed.mapNotNull { receivers.remove(it) }
                                        rooms.value = Rooms(publisher, receivers.values.toList(), identity)
                                        closeRooms(retired.map { it.room })
                                        var creationFailed = false
                                        val added = connections.filterKeys { it !in receivers }
                                        for ((url, parameters) in added) {
                                            try {
                                                receivers[url] = createRoom(parameters)
                                            } catch (cancelled: CancellationException) {
                                                throw cancelled
                                            } catch (_: Exception) {
                                                creationFailed = true
                                            }
                                        }
                                        rooms.value =
                                            Rooms(publisher, receivers.values.toList(), identity, creationFailed)
                                        for ((url, parameters) in added) {
                                            receivers[url]?.let { receiver ->
                                                launch { connectRoom(receiver, parameters) }
                                            }
                                        }
                                    }
                            } finally {
                                rooms.value = null
                                withContext(NonCancellable) {
                                    closeRooms(receivers.values.map { it.room } + publisher.room)
                                }
                            }
                        }
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                publishingFailed.value = true
            }
        }
        scope.launch {
            combine(rooms, rtc.publishing) { current, publishing -> current to publishing }
                .collect { (current, publishing) ->
                    val info = publishing?.encryptionInfo
                    if (current != null && publishing != null && info != null) {
                        for (connection in listOf(current.publishing) + current.receiving) {
                            if (owns(connection)) {
                                try {
                                    connection.keyProvider?.update(info, publishing.userId)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    disconnectFailedRoom(connection)
                                }
                            }
                        }
                    }
                }
        }
    }

    private suspend fun createRoom(parameters: LiveKitCallParameters): RoomConnection {
        val encryptionInfo = parameters.encryptionInfo
        val encryptionOptions = if (encryptionInfo == null) null else E2EEOptions(MatrixRtcKeyProvider.options)
        val room = liveKit.create(RoomOptions(e2eeOptions = encryptionOptions))
        try {
            val provider = if (encryptionInfo == null) null else MatrixRtcKeyProvider(checkNotNull(room.keyProvider))
            if (encryptionInfo != null) {
                checkNotNull(provider).update(encryptionInfo, parameters.userId)
            }
            return RoomConnection(room, provider)
        } catch (failure: Throwable) {
            closeRooms(listOf(room))
            throw failure
        }
    }

    private fun owns(connection: RoomConnection): Boolean {
        return rooms.value?.let { current ->
            current.publishing === connection || current.receiving.any { it === connection }
        } == true
    }

    private suspend fun connectRoom(connection: RoomConnection, parameters: LiveKitCallParameters) {
        if (!owns(connection)) return
        try {
            connection.room.connect(parameters.url, parameters.token)
            // A platform connection attempt may finish after the room was removed.
            if (!owns(connection)) {
                withContext(NonCancellable) { connection.room.disconnect() }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            disconnectFailedRoom(connection)
        }
    }

    private suspend fun disconnectFailedRoom(connection: RoomConnection) {
        if (!owns(connection)) return
        if (rooms.value?.publishing === connection) publishingFailed.value = true
        withContext(NonCancellable) {
            runCatching { connection.room.disconnect() }
                .onFailure { log.warn(it) { "Could not disconnect a failed LiveKit room" } }
        }
    }

    private suspend fun closeRooms(rooms: List<Room>) {
        withContext(NonCancellable) {
            for (room in rooms) {
                try {
                    try {
                        room.disconnect()
                    } finally {
                        withContext(NonCancellable) { room.release() }
                    }
                } catch (failure: Exception) {
                    log.warn(failure) { "Could not release a LiveKit room" }
                }
            }
        }
    }

    private data class MediaFlags(val connected: CallConnectionState, val unavailable: Boolean)

    private val flags = rooms.flatMapLatest { r ->
        if (r == null) flowOf(MediaFlags(CallConnectionState.CONNECTING, false))
        else
            combine(
                r.publishing.room.state,
                combineOrEmpty(
                    r.receiving.map { receiver ->
                        receiver.room.state.map { connection ->
                            connection == Room.State.DISCONNECTED || connection == Room.State.RECONNECTING
                        }
                    }
                ),
            ) { connection, receiving ->
                MediaFlags(
                    when {
                        connection == Room.State.RECONNECTING -> CallConnectionState.RECONNECTING
                        connection == Room.State.CONNECTED -> CallConnectionState.CONNECTED
                        else -> CallConnectionState.CONNECTING
                    },
                    r.receivingCreationFailed || receiving.any { it },
                )
            }
    }

    private data class Person(
        val id: String,
        val microphone: Boolean,
        val speaking: Boolean,
        val lastSpoke: Long,
        val video: LiveKitCallVideo?,
        val hasPublications: Boolean,
    )

    private fun participant(room: Room, participant: Participant, localIdentity: String): Flow<Person?> {
        val video =
            participant.trackPublications.flatMapLatest { publications ->
                val camera = publications.values.firstOrNull { it.source == Track.Source.CAMERA }
                if (camera == null) flowOf(null)
                else
                    combine(camera.track, camera.muted) { track, muted ->
                        if (track != null && !muted)
                            LiveKitCallVideo(room, TrackReference(participant, camera, Track.Source.CAMERA))
                        else null
                    }
            }
        val publications = participant.trackPublications.map { it.isNotEmpty() }
        return combine(
            participant.identity,
            participant.isMicrophoneEnabled,
            participant.isSpeaking,
            participant.lastSpokeAt,
            combine(video, publications) { track, published -> track to published },
        ) { identity, microphone, speaking, lastSpoke, (track, published) ->
            val id = if (participant === room.localParticipant) localIdentity else identity?.value
            id?.let { Person(it, microphone, speaking, lastSpoke ?: 0L, track, published) }
        }
    }

    private val participants = rooms.flatMapLatest { r ->
        if (r == null) flowOf(emptyList())
        else {
            val all = (listOf(r.publishing) + r.receiving).map { it.room }
            combineOrEmpty(
                    all.map { room ->
                        room.remoteParticipants.map { remote ->
                            remote.values.toList() +
                                if (room === r.publishing.room) listOf(room.localParticipant) else emptyList()
                        }
                    }
                )
                .flatMapLatest { lists ->
                    combineOrEmpty(
                        lists.flatMapIndexed { index, list ->
                            list.map { participant(all[index], it, r.localIdentity) }
                        }
                    )
                }
                .combine(rtc.members) { people, members ->
                    val byId =
                        people
                            .filterNotNull()
                            .sortedByDescending { it.hasPublications }
                            .distinctBy { it.id }
                            .associateBy { it.id }
                    members
                        .map { (member, user) ->
                            val id = matrixRtcParticipantIdentity(member.first, member.second)
                            val person = byId[id]
                            CallParticipant(
                                id,
                                user,
                                id == r.localIdentity,
                                person?.microphone ?: false,
                                person?.speaking ?: false,
                                person?.video,
                            ) to (person?.lastSpoke ?: 0L)
                        }
                        .sortedWith(
                            compareBy<Pair<CallParticipant, Long>> { it.first.isByMe }.thenByDescending { it.second }
                        )
                        .map { it.first }
                }
        }
    }
    override val state =
        combine(
                flags.combine(publishingFailed) { flags, failed ->
                    if (failed) flags.copy(connected = CallConnectionState.FAILED) else flags
                },
                participants,
                controls.microphoneEnabled,
                controls.cameraEnabled,
                controls.error,
            ) { flags, participants, microphone, camera, error ->
                CallMediaState(
                    connection = flags.connected,
                    participants = participants,
                    microphoneEnabled = microphone,
                    cameraEnabled = camera,
                    error =
                        error ?: if (flags.connected == CallConnectionState.FAILED) CallMediaError.CONNECTION else null,
                    participantsUnavailable = flags.unavailable,
                )
            }
            .stateIn(scope, SharingStarted.Eagerly, CallMediaState())

    override fun toggleMicrophone() {
        controls.toggle(CallPermissionKind.MICROPHONE)
    }

    override fun toggleCamera() {
        controls.toggle(CallPermissionKind.CAMERA)
    }

    override fun onPermissionRequestResult(kind: CallPermissionKind, granted: Boolean) {
        controls.onPermissionRequestResult(kind, granted)
    }

    private suspend fun bindMedia(room: Room): Unit = coroutineScope {
        launch { bindMicrophone(room) }
        launch { bindCamera(room) }
    }

    private suspend fun bindMicrophone(room: Room) {
        val participant = room.localParticipant
        combine(
                controls.microphoneEnabled,
                controls.microphonePermissionGranted,
                room.state,
                participant.isMicrophoneEnabled,
            ) { desired, granted, connection, actual ->
                when {
                    desired == actual -> null
                    !desired -> false
                    granted && connection == Room.State.CONNECTED -> true
                    else -> null
                }
            }
            .distinctUntilChanged()
            .collectLatest { enabled ->
                if (enabled == null) return@collectLatest
                try {
                    if (!participant.setMicrophoneEnabled(enabled)) {
                        controls.onMediaChangeFailure(
                            CallPermissionKind.MICROPHONE,
                            enabled,
                            participant.isMicrophoneEnabled.value,
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    controls.onMediaChangeFailure(
                        CallPermissionKind.MICROPHONE,
                        enabled,
                        participant.isMicrophoneEnabled.value,
                    )
                }
            }
    }

    private suspend fun bindCamera(room: Room) {
        val participant = room.localParticipant
        combine(controls.cameraEnabled, controls.cameraPermissionGranted, room.state, participant.isCameraEnabled) {
                desired,
                granted,
                connection,
                actual ->
                when {
                    desired == actual -> null
                    !desired -> false
                    granted && connection == Room.State.CONNECTED -> true
                    else -> null
                }
            }
            .distinctUntilChanged()
            .collectLatest { enabled ->
                if (enabled == null) return@collectLatest
                try {
                    if (!participant.setCameraEnabled(enabled)) {
                        controls.onMediaChangeFailure(
                            CallPermissionKind.CAMERA,
                            enabled,
                            participant.isCameraEnabled.value,
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    controls.onMediaChangeFailure(CallPermissionKind.CAMERA, enabled, participant.isCameraEnabled.value)
                }
            }
    }
}

private inline fun <reified T> combineOrEmpty(flows: List<Flow<T>>): Flow<List<T>> {
    return if (flows.isEmpty()) flowOf(emptyList()) else combine(flows) { it.toList() }
}
