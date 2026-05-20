package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.clientserverapi.model.room.GetPublicRoomsWithFilter
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.EnterRoom
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface SearchGroupViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onBack: () -> Unit,
        onGroupJoined: (UserId, RoomId) -> Unit,
        onGroupKnocked: (RoomId) -> Unit,
    ): SearchGroupViewModel = SearchGroupViewModelImpl(viewModelContext, onBack, onGroupJoined, onGroupKnocked)

    companion object : SearchGroupViewModelFactory
}

interface SearchGroupViewModel {
    val searchTerm: TextFieldViewModel
    val groupSearchInProgress: StateFlow<Boolean>

    @Deprecated(
        message = "Use `enterGroupInProgress` instead after switching to `enterGroup`",
        replaceWith = ReplaceWith("enterGroupInProgress"),
    )
    val joinGroupInProgress: StateFlow<Boolean>
    val enterGroupInProgress: StateFlow<Boolean>
    val error: StateFlow<String?>
    val foundGroups: StateFlow<List<SearchGroup>>

    fun enterGroup(roomId: RoomId, reason: String? = null)

    @Deprecated(
        message = "`joinGroup` only handles Public rooms, use `enterGroup` instead.",
        replaceWith = ReplaceWith("enterGroup(roomId)"),
    )
    fun joinGroup(roomId: RoomId)

    fun back()

    data class SearchGroup(
        val roomId: RoomId,
        val groupName: String,
        val topic: String?,
        val image: StateFlow<ByteArray?>,
        val initials: String,
        val joinedMembersCount: Long,
        val joinRule: JoinRule,
    )
}

@OptIn(FlowPreview::class)
class SearchGroupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onBack: () -> Unit,
    private val onGroupJoined: (UserId, RoomId) -> Unit,
    private val onGroupKnocked: (RoomId) -> Unit,
) : SearchGroupViewModel, MatrixClientViewModelContext by viewModelContext {

    private val backCallback = BackCallback { back() }

    init {
        registerBackCallback(backCallback)
    }

    private val initials = get<Initials>()
    private val enterRoom = get<EnterRoom>()

    override val searchTerm = TextFieldViewModelImpl(maxLength = 1_000)
    override val groupSearchInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Deprecated(
        "Use `enterGroupInProgress` instead after switching to `enterGroup`",
        replaceWith = ReplaceWith("enterGroupInProgress"),
    )
    override val joinGroupInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val enterGroupInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val foundGroups: StateFlow<List<SearchGroupViewModel.SearchGroup>> =
        searchTerm
            .map { it.text }
            .distinctUntilChanged()
            .debounce(300.milliseconds)
            .scopedMapLatest { searchTerm ->
                groupSearchInProgress.update { true }
                try {
                    matrixClient.api.room
                        .getPublicRooms(
                            filter =
                                if (searchTerm.isBlank()) null
                                else GetPublicRoomsWithFilter.Request.Filter(genericSearchTerm = searchTerm)
                        )
                        .fold(
                            onFailure = {
                                log.error(it) { "cannot search for public rooms" }
                                error.value = i18n.searchGroupFailedSearch()
                                emptyList()
                            },
                            onSuccess = { getPublicRoomsResponse ->
                                log.debug { getPublicRoomsResponse }
                                getPublicRoomsResponse.chunk.map { publicRoomsChunk ->
                                    val groupName = publicRoomsChunk.name ?: i18n.commonUnknown()
                                    SearchGroupViewModel.SearchGroup(
                                        roomId = publicRoomsChunk.roomId,
                                        groupName = groupName,
                                        topic = publicRoomsChunk.topic,
                                        image =
                                            publicRoomsChunk.avatarUrl?.let { url ->
                                                matrixClient.media
                                                    .getThumbnail(url, avatarSize().toLong(), avatarSize().toLong())
                                                    .getOrNull()
                                                    ?.stateIn(this)
                                            } ?: MutableStateFlow(null),
                                        initials = initials.compute(groupName),
                                        joinedMembersCount = publicRoomsChunk.joinedMembersCount,
                                        joinRule = publicRoomsChunk.joinRule,
                                    )
                                }
                            },
                        )
                } finally {
                    groupSearchInProgress.update { false }
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    @Deprecated(
        "`joinGroup` only handles Public rooms, use `enterGroup` instead.",
        replaceWith = ReplaceWith("enterGroup(roomId)"),
    )
    override fun joinGroup(roomId: RoomId) {
        coroutineScope.launch {
            enterGroupInProgress.update { true }
            try {
                matrixClient.api.room
                    .joinRoom(roomId)
                    .onFailure {
                        log.error(it) { "cannot join room" }
                        error.value = i18n.searchGroupJoinFailedGeneric()
                    }
                    .onSuccess {
                        onGroupJoined(userId, roomId)
                        error.value = null
                    }
            } finally {
                enterGroupInProgress.update { false }
            }
        }
    }

    override fun enterGroup(roomId: RoomId, reason: String?) {
        if (enterGroupInProgress.getAndUpdate { true }) {
            log.warn { "Adding room ($roomId) is already in progress" }
            return
        }

        val searchGroup = foundGroups.value.firstOrNull { it.roomId == roomId }

        if (searchGroup == null) {
            log.warn { "Tried to add unknown group ($roomId)" }
            return
        }

        coroutineScope
            .launch {
                enterRoom(i18n, matrixClient, searchGroup.joinRule, roomId, reason)
                    .fold(
                        onSuccess = {
                            when (it.kind) {
                                is JoinRule.Public,
                                is JoinRule.Invite,
                                is JoinRule.Restricted -> {
                                    onGroupJoined(userId, roomId)
                                }

                                is JoinRule.Knock -> {
                                    onGroupKnocked(roomId)
                                }

                                else -> {
                                    log.warn {
                                        "Join rule (${it.kind.name}) for room (${roomId}) succeeded. This shouldn't happen"
                                    }
                                }
                            }

                            error.value = null
                        },
                        onFailure = { error.value = it.reason },
                        onError = {
                            log.error(it.error) {
                                "Failed to join room $roomId using ${it.kind} (room has ${searchGroup.joinRule})"
                            }
                            error.value = i18n.searchGroupJoinFailedGeneric()
                        },
                    )
            }
            .invokeOnCompletion { enterGroupInProgress.value = false }
    }

    override fun back() {
        onBack()
    }
}
