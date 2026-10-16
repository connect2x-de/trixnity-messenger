package de.connect2x.trixnity.messenger.viewmodel.room.call

import com.arkivanov.essenty.lifecycle.doOnDestroy
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.rtc
import de.connect2x.trixnity.client.rtc.PerMemberRtcSlotMembershipEncryptionInfo
import de.connect2x.trixnity.client.rtc.RtcSlotMembership
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.MSC4195
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.core.model.events.m.rtc.LiveKitRtcTransport
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberEventContent
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberId
import de.connect2x.trixnity.core.model.events.m.rtc.RtcSlotId
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel.*
import de.connect2x.trixnity.messenger.viewmodel.toRoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

@TrixnityMessengerCallApi
interface RoomCallViewModel {
    val roomInfo: StateFlow<RoomInfoElement?>
    val publishing: StateFlow<LiveKitCallParameters?>

    /** this will never include the url from [publishing] */
    val receiving: StateFlow<List<LiveKitCallParameters>>
    val members: StateFlow<Map<Pair<UserId, RtcMemberId>, UserInfoElement>>
    val error: StateFlow<String?>

    fun back()

    @OptIn(MSC4143::class)
    data class LiveKitCallParameters(
        val url: String,
        val token: String,
        val userId: UserId,
        val memberId: RtcMemberId,
        // TODO this is currently the exact same for all instances of LiveKitCallParameters.
        //      We should be RtcEncryptionKeyEventContent for the specific participant instead.
        val encryptionInfo: PerMemberRtcSlotMembershipEncryptionInfo? = null,
    )
}

@OptIn(MSC4143::class, MSC4195::class, TrixnityMessengerCallApi::class)
internal class RoomCallViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onBack: () -> Unit,
) : RoomCallViewModel, MatrixClientViewModelContext by viewModelContext {
    private val configuration = viewModelContext.get<MatrixMessengerConfiguration>()
    private val initials: Initials = viewModelContext.get<Initials>()
    private val maxThumbnailSize = configuration.downloadLimits.thumbnail
    private val getRoomName = viewModelContext.get<RoomName>()

    private val slotId: RtcSlotId = CallRtcApplication.SLOT_ID
    private val membership = MutableStateFlow<RtcSlotMembership?>(null)

    override val error = MutableStateFlow<String?>(null)

    override val roomInfo: StateFlow<RoomInfoElement?> =
        combine(matrixClient.room.getById(roomId), getRoomName.getRoomName(roomId, matrixClient)) { room, name ->
                room?.toRoomInfoElement(coroutineScope, initials, matrixClient, name, maxThumbnailSize)
                    ?: RoomInfoElement(name, roomId, initials.compute(name), null)
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    init {
        check(configuration.features.enableVideoCalls) {
            "RoomCallViewModel must not be used without video calls enabled"
        }
        registerBackCallback(BackCallback(onBack = ::back))
        lifecycle.doOnDestroy { leave() }
        coroutineScope.launch { matrixClient.user.loadMembers(roomId, wait = false) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val publishing: StateFlow<LiveKitCallParameters?> =
        membership
            .filterNotNull()
            .flatMapLatest { joined ->
                joined.state.flatMapLatest { state ->
                    when (state) {
                        is RtcSlotMembership.State.Join -> {
                            error.value = null
                            val transport =
                                state.content.transports
                                    ?.published
                                    ?.filterIsInstance<LiveKitRtcTransport>()
                                    ?.firstOrNull() ?: return@flatMapLatest flowOf(null)
                            val parameters =
                                credentials(transport.url, transport.serverName ?: userId.domain, joined.memberId)
                                    .getOrThrow()
                            joined.encryptionInfo.map { info ->
                                parameters.copy(encryptionInfo = info as? PerMemberRtcSlotMembershipEncryptionInfo)
                            }
                        }

                        is RtcSlotMembership.State.JoiningError -> {
                            reportMembershipError(state.error)
                            flowOf(null)
                        }

                        is RtcSlotMembership.State.LeavingError -> {
                            reportMembershipError(state.error)
                            flowOf(null)
                        }

                        is RtcSlotMembership.State.Joining,
                        is RtcSlotMembership.State.Leaving,
                        is RtcSlotMembership.State.Leave -> flowOf(null)
                    }
                }
            }
            .catch { exception ->
                log.warn(exception) { "Room call failed in $roomId" }
                error.value = i18n.callConnectError()
                emit(null)
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val receiving: StateFlow<List<LiveKitCallParameters>> =
        combine(publishing, matrixClient.rtc.getMembers(roomId, slotId)) { publisher, members ->
                if (publisher == null) return@combine emptyList()
                members
                    .flatMap { (member, event) ->
                        (event.content as? RtcMemberEventContent.Join)
                            ?.transports
                            ?.published
                            .orEmpty()
                            .filterIsInstance<LiveKitRtcTransport>()
                            .map { it.url to (it.serverName ?: member.first.domain) }
                    }
                    .filter { (url, _) -> url.toString() != publisher.url }
                    .distinctBy { it.first }
                    .mapNotNull { (url, server) ->
                        credentials(url, server, publisher.memberId)
                            .getOrNull()
                            ?.copy(encryptionInfo = publisher.encryptionInfo)
                    }
            }
            .catch { exception ->
                log.warn(exception) { "Could not receive room call in $roomId" }
                error.value = i18n.callConnectError()
            }
            .stateIn(coroutineScope, WhileSubscribed(), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val members: StateFlow<Map<Pair<UserId, RtcMemberId>, UserInfoElement>> =
        matrixClient.rtc
            .getMembers(roomId, slotId)
            .map { members -> members.filterValues { it.content is RtcMemberEventContent.Join }.keys }
            .distinctUntilChanged()
            .mapLatest { members ->
                members.associateWith { (userId, _) ->
                    matrixClient.user
                        .getById(roomId, userId)
                        .first()
                        .toUserInfoElement(
                            coroutineScope,
                            matrixClient,
                            initials,
                            fallbackUserId = userId,
                            maxMediaSizeInMemory = maxThumbnailSize,
                        )
                }
            }
            .stateIn(coroutineScope, WhileSubscribed(), emptyMap())

    override fun back() {
        leave()
        onBack()
    }

    private fun leave() {
        membership.value?.leave(null)
    }

    private fun reportMembershipError(failure: RtcSlotMembership.Error) {
        val exception =
            when (failure) {
                is RtcSlotMembership.Error.NetworkError -> failure.error
                is RtcSlotMembership.Error.PermissionError -> failure.error
            }
        log.warn(exception) { "Room call membership failed in $roomId" }
        error.value = i18n.callConnectError()
    }

    private suspend fun credentials(url: Url, server: String, memberId: RtcMemberId): Result<LiveKitCallParameters> {
        return matrixClient.api.rtc
            .getLiveKitToken(serverName = server, url = url, roomId = roomId, slotId = slotId, memberId = memberId)
            .onFailure { if (it is CancellationException) throw it }
            .map { LiveKitCallParameters(url.toString(), it, userId, memberId) }
    }
}
