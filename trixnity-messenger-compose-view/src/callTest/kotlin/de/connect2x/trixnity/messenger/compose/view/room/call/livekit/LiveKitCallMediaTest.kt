package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import de.connect2x.livekitnity.api.ConnectOptions
import de.connect2x.livekitnity.api.LiveKit
import de.connect2x.livekitnity.api.RoomOptions
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.participant.LocalParticipant
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.RemoteParticipant
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.TrackPublication
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallConnectionState
import de.connect2x.trixnity.messenger.call.CallPermissionKind
import de.connect2x.trixnity.messenger.call.CallSignalling
import de.connect2x.trixnity.messenger.call.CallSignalling.LiveKitCallParameters
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

@OptIn(MSC4143::class, TrixnityMessengerCallApi::class)
class LiveKitCallMediaTest {
    @Test
    fun localChoicesApplyAfterConnectingAndMuteDuringReconnection() = runTest {
        val rtc = TestSignalling()
        rtc.publishing.value = null
        val liveKit = TestLiveKit()
        val permissions = mutableListOf<CallPermissionKind>()
        val media = LiveKitCallMedia(backgroundScope, liveKit, rtc, permissions::add)
        media.toggleMicrophone()
        media.onPermissionRequestResult(CallPermissionKind.MICROPHONE, true)
        delay(2.seconds)
        assertTrue(media.state.value.microphoneEnabled)
        assertTrue(liveKit.created.isEmpty())
        rtc.publishing.value = parameters("publisher")
        delay(2.seconds)
        val publisher = liveKit.room("publisher")
        assertTrue(publisher.localParticipant.isMicrophoneEnabled.value)
        publisher.state.value = Room.State.RECONNECTING
        delay(2.seconds)
        media.toggleMicrophone()
        delay(2.seconds)
        assertFalse(publisher.localParticipant.isMicrophoneEnabled.value)
        assertFalse(media.state.value.microphoneEnabled)
        assertEquals(listOf(CallPermissionKind.MICROPHONE), permissions)
        media.toggleCamera()
        media.onPermissionRequestResult(CallPermissionKind.CAMERA, true)
        delay(2.seconds)
        assertTrue(media.state.value.cameraEnabled)
        assertFalse(publisher.localParticipant.isCameraEnabled.value)
        publisher.state.value = Room.State.CONNECTED
        delay(2.seconds)
        assertTrue(publisher.localParticipant.isCameraEnabled.value)
    }

    @Test
    fun failedOrReconnectingReceiversDoNotDisablePublishingControls() = runTest {
        val rtc = TestSignalling()
        rtc.receiving.value = listOf(parameters("unavailable"), parameters("receiver"))
        val liveKit = TestLiveKit(failingUrl = "unavailable")
        val media = LiveKitCallMedia(backgroundScope, liveKit, rtc) {}
        delay(2.seconds)
        media.toggleMicrophone()
        media.onPermissionRequestResult(CallPermissionKind.MICROPHONE, true)
        delay(2.seconds)
        val publisher = liveKit.room("publisher")
        assertTrue(publisher.localParticipant.isMicrophoneEnabled.value)
        assertEquals(CallConnectionState.CONNECTED, media.state.value.connection)
        assertTrue(media.state.value.participantsUnavailable)
        liveKit.room("receiver").state.value = Room.State.RECONNECTING
        delay(2.seconds)
        assertEquals(CallConnectionState.CONNECTED, media.state.value.connection)
        media.toggleMicrophone()
        delay(2.seconds)
        assertFalse(publisher.localParticipant.isMicrophoneEnabled.value)
    }

    @Test
    fun publisherFailureKeepsMediaChoicesAndAReplacementPublisherClearsTheError() = runTest {
        val rtc = TestSignalling()
        rtc.publishing.value = parameters("unavailable")
        val liveKit = TestLiveKit(failingUrl = "unavailable")
        val media = LiveKitCallMedia(backgroundScope, liveKit, rtc) {}
        delay(2.seconds)
        assertEquals(CallConnectionState.FAILED, media.state.value.connection)
        media.toggleMicrophone()
        media.onPermissionRequestResult(CallPermissionKind.MICROPHONE, true)
        delay(2.seconds)
        assertTrue(media.state.value.microphoneEnabled)
        rtc.publishing.value = parameters("publisher")
        delay(2.seconds)
        assertEquals(CallConnectionState.CONNECTED, media.state.value.connection)
        assertTrue(liveKit.room("publisher").localParticipant.isMicrophoneEnabled.value)
    }

    @Test
    fun receivingServerChangesRetainUnrelatedConnectionsAndDisposeRemovedRooms() = runTest {
        val rtc = TestSignalling()
        rtc.receiving.value = listOf(parameters("a"), parameters("b"))
        val liveKit = TestLiveKit()
        LiveKitCallMedia(backgroundScope, liveKit, rtc) {}
        delay(2.seconds)
        val a = liveKit.room("a")
        val b = liveKit.room("b")
        rtc.receiving.value = listOf(parameters("a"), parameters("b"), parameters("c"))
        delay(2.seconds)
        assertEquals(4, liveKit.created.size)
        assertEquals(0, a.releases)
        assertEquals(0, b.releases)
        rtc.receiving.value = listOf(parameters("a"), parameters("c"))
        delay(2.seconds)
        assertEquals(4, liveKit.created.size)
        assertEquals(0, a.releases)
        assertEquals(1, b.releases)
        backgroundScope.cancel()
        delay(2.seconds)
        assertTrue(liveKit.created.all { it.releases == 1 })
    }

    @Test
    fun removingAConnectingReceiverAlsoDisconnectsALateConnectionResult() = runTest {
        val rtc = TestSignalling()
        rtc.receiving.value = listOf(parameters("pending"), parameters("stable"))
        val liveKit = TestLiveKit(delayedUrl = "pending")
        val media = LiveKitCallMedia(backgroundScope, liveKit, rtc) {}
        delay(2.seconds)
        val pending = liveKit.room("pending")
        val stable = liveKit.room("stable")
        assertEquals(Room.State.CONNECTING, pending.state.value)
        rtc.receiving.value = listOf(parameters("stable"))
        delay(2.seconds)
        assertEquals(1, pending.releases)
        assertEquals(0, stable.releases)
        pending.continueConnection.value = true
        delay(2.seconds)
        assertEquals(Room.State.DISCONNECTED, pending.state.value)
        assertEquals(1, pending.releases)
        assertEquals(CallConnectionState.CONNECTED, media.state.value.connection)
    }

    @Test
    fun replacingThePublisherReleasesItsRoomsAndReappliesLocalMediaChoices() = runTest {
        val rtc = TestSignalling()
        rtc.receiving.value = listOf(parameters("receiver"))
        val liveKit = TestLiveKit()
        val media = LiveKitCallMedia(backgroundScope, liveKit, rtc) {}
        delay(2.seconds)
        val previousPublisher = liveKit.room("publisher")
        val previousReceiver = liveKit.room("receiver")
        media.toggleMicrophone()
        media.onPermissionRequestResult(CallPermissionKind.MICROPHONE, true)
        delay(2.seconds)
        rtc.publishing.value = parameters("new-publisher")
        delay(2.seconds)
        assertEquals(1, previousPublisher.releases)
        assertEquals(1, previousReceiver.releases)
        assertTrue(liveKit.room("new-publisher").localParticipant.isMicrophoneEnabled.value)
        backgroundScope.cancel()
        delay(2.seconds)
        assertTrue(liveKit.created.all { it.releases == 1 })
    }

    @Test
    fun audioPublicationWinsOverAReceiveOnlyCopyOfTheSameParticipant() = runTest {
        val rtc = TestSignalling()
        rtc.receiving.value = listOf(parameters("receiver"))
        val remoteUser = UserId("bob", "example.org")
        val member = RtcMemberId("remote")
        val identity = matrixRtcParticipantIdentity(remoteUser, member)
        rtc.members.value = mapOf((remoteUser to member) to UserInfoElement(remoteUser, "Bob", "B"))
        val liveKit = TestLiveKit()
        val media = LiveKitCallMedia(backgroundScope, liveKit, rtc) {}
        delay(2.seconds)
        val receiveOnly = TestParticipant(identity)
        val publishing =
            TestParticipant(identity).apply {
                isMicrophoneEnabled.value = true
                isSpeaking.value = true
                lastSpokeAt.value = 10L
            }
        liveKit.room("publisher").remoteParticipants.value = mapOf(Participant.Identity(identity) to receiveOnly)
        liveKit.room("receiver").remoteParticipants.value = mapOf(Participant.Identity(identity) to publishing)
        delay(2.seconds)
        assertFalse(media.state.value.participants.single().microphoneEnabled)
        publishing.trackPublications.value = mapOf("audio" to TestAudioPublication())
        delay(2.seconds)
        val participant = media.state.value.participants.single()
        assertTrue(participant.microphoneEnabled)
        assertTrue(participant.isSpeaking)
    }
}

@OptIn(MSC4143::class, TrixnityMessengerCallApi::class)
private fun parameters(url: String) =
    LiveKitCallParameters(url, "token", UserId("alice", "example.org"), RtcMemberId("local"))

@OptIn(MSC4143::class, TrixnityMessengerCallApi::class)
private class TestSignalling : CallSignalling {
    override val publishing = MutableStateFlow<LiveKitCallParameters?>(parameters("publisher"))
    override val receiving = MutableStateFlow(emptyList<LiveKitCallParameters>())
    override val members = MutableStateFlow(emptyMap<Pair<UserId, RtcMemberId>, UserInfoElement>())
    override val roomInfo = MutableStateFlow<RoomInfoElement?>(null)
    override val error = MutableStateFlow<String?>(null)

    override fun close() {}
}

private class TestLiveKit(private val failingUrl: String? = null, private val delayedUrl: String? = null) : LiveKit {
    val created = mutableListOf<TestRoom>()

    override fun create(options: RoomOptions): Room = TestRoom(failingUrl, delayedUrl).also { created += it }

    override fun createCameraEnumerator() = error("Not used by call media")

    fun room(url: String) = created.single { it.url == url }
}

private class TestRoom(private val failingUrl: String?, private val delayedUrl: String?) : Room {
    var url: String? = null
    var releases = 0
    val continueConnection = MutableStateFlow(false)
    override val state = MutableStateFlow(Room.State.DISCONNECTED)
    override val localParticipant = TestParticipant("local")
    override val remoteParticipants = MutableStateFlow(emptyMap<Participant.Identity, RemoteParticipant>())
    override val keyProvider = null
    override val audioSwitchHandler = null
    override val events
        get() = error("Not used by call media")

    override val sid
        get() = error("Not used by call media")

    override val name
        get() = error("Not used by call media")

    override val metadata
        get() = error("Not used by call media")

    override val isRecording
        get() = error("Not used by call media")

    override val activeSpeakers
        get() = error("Not used by call media")

    override suspend fun prepareConnection(url: String, token: String?) {}

    override suspend fun connect(url: String, token: String, options: ConnectOptions) {
        this.url = url
        if (url == failingUrl) error("Receiver connection failed")
        if (url == delayedUrl) {
            state.value = Room.State.CONNECTING
            continueConnection.first { it }
        }
        state.value = Room.State.CONNECTED
    }

    override suspend fun disconnect() {
        state.value = Room.State.DISCONNECTED
    }

    override suspend fun release() {
        releases++
    }
}

private class TestParticipant(id: String) : LocalParticipant, RemoteParticipant {
    override val identity = MutableStateFlow<Participant.Identity?>(Participant.Identity(id))
    override val isMicrophoneEnabled = MutableStateFlow(false)
    override val isCameraEnabled = MutableStateFlow(false)
    override val isScreenShareEnabled = MutableStateFlow(false)
    override val isSpeaking = MutableStateFlow(false)
    override val lastSpokeAt = MutableStateFlow<Long?>(null)
    override val trackPublications = MutableStateFlow(emptyMap<String, TrackPublication>())
    override val events
        get() = error("Not used by call media")

    override val sid
        get() = error("Not used by call media")

    override val audioLevel
        get() = error("Not used by call media")

    override val name
        get() = error("Not used by call media")

    override val metadata
        get() = error("Not used by call media")

    override val attributes
        get() = error("Not used by call media")

    override val permissions
        get() = error("Not used by call media")

    override val connectionQuality
        get() = error("Not used by call media")

    override val kind
        get() = error("Not used by call media")

    override suspend fun setMicrophoneEnabled(enabled: Boolean): Boolean {
        isMicrophoneEnabled.value = enabled
        return true
    }

    override suspend fun setCameraEnabled(enabled: Boolean): Boolean {
        isCameraEnabled.value = enabled
        return true
    }

    override suspend fun setScreenShareEnabled(enabled: Boolean): Boolean {
        isScreenShareEnabled.value = enabled
        return true
    }
}

private class TestAudioPublication : TrackPublication {
    override val source = Track.Source.MICROPHONE
    override val track = MutableStateFlow<Track?>(null)
    override val muted = MutableStateFlow(false)
    override val name = "audio"
    override val sid = "audio"
    override val kind = Track.Kind.AUDIO
    override val subscribed = true
    override val simulcasted = null
    override val dimensions = null
    override val mimeType = null
    override val events
        get() = error("Not used by call media")
}
