package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.util.JoinRoom
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import io.github.oshai.kotlinlogging.KotlinLogging
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
import net.folivo.trixnity.client.media
import net.folivo.trixnity.clientserverapi.model.rooms.GetPublicRoomsWithFilter
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

interface SearchGroupViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onBack: () -> Unit,
        onGroupJoined: (UserId, RoomId) -> Unit,
        onGroupKnocked: (RoomId) -> Unit,
    ): SearchGroupViewModel =
        SearchGroupViewModelImpl(viewModelContext, onBack, onGroupJoined, onGroupKnocked)

    companion object : SearchGroupViewModelFactory
}

interface SearchGroupViewModel {
    val searchTerm: TextFieldViewModel
    val groupSearchInProgress: StateFlow<Boolean>
    val addGroupInProgress: StateFlow<Boolean>
    val error: StateFlow<String?>
    val foundGroups: StateFlow<List<SearchGroup>>

    fun addGroup(searchGroup: SearchGroup, reason: String? = null)
    fun back()

    data class SearchGroup(
        val roomId: RoomId,
        val groupName: String,
        val topic: String?,
        val image: StateFlow<ByteArray?>,
        val initials: String,
        val joinedMembersCount: Long,
        val joinRule: JoinRule
    )
}

@OptIn(FlowPreview::class)
class SearchGroupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onBack: () -> Unit,
    private val onGroupJoined: (UserId, RoomId) -> Unit,
    private val onGroupKnocked: (RoomId) -> Unit
) : SearchGroupViewModel, MatrixClientViewModelContext by viewModelContext {

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)
    }

    private val initials = get<Initials>()
    private val joinRoom = get<JoinRoom>()

    override val searchTerm = TextFieldViewModelImpl()
    override val groupSearchInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val addGroupInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val foundGroups: StateFlow<List<SearchGroupViewModel.SearchGroup>> = searchTerm
        .map { it.text }
        .distinctUntilChanged()
        .debounce(300.milliseconds)
        .scopedMapLatest { searchTerm ->
            groupSearchInProgress.update { true }
            try {
                matrixClient.api.room.getPublicRooms(
                    filter = if (searchTerm.isBlank()) null
                    else GetPublicRoomsWithFilter.Request.Filter(genericSearchTerm = searchTerm),
                ).fold(
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
                                image = publicRoomsChunk.avatarUrl?.let { url ->
                                    matrixClient.media.getThumbnail(
                                        url,
                                        avatarSize().toLong(),
                                        avatarSize().toLong(),
                                    ).getOrNull()?.stateIn(this)
                                } ?: MutableStateFlow(null),
                                initials = initials.compute(groupName),
                                joinedMembersCount = publicRoomsChunk.joinedMembersCount,
                                joinRule = publicRoomsChunk.joinRule
                            )
                        }
                    }
                )
            } finally {
                groupSearchInProgress.update { false }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun addGroup(searchGroup: SearchGroupViewModel.SearchGroup, reason: String?) {
        if (addGroupInProgress.getAndUpdate { true }) {
            log.warn { "Adding room (${searchGroup.roomId}) is already in progress" }
            return
        }

        coroutineScope.launch {
            joinRoom(
                matrixClient,
                searchGroup.roomId,
                searchGroup.joinRule,
                reason
            ).fold(
                onSuccess = {
                    when (it.kind) {
                        is JoinRule.Public -> {
                            onGroupJoined(userId, searchGroup.roomId)
                        }

                        is JoinRule.Knock -> {
                            onGroupKnocked(searchGroup.roomId)
                        }

                        else -> {
                            log.warn { "Join rule (${it.kind.name}) for room (${searchGroup.roomId}) succeeded. This shouldn't happen" }
                        }
                    }

                    error.value = null
                },
                onFailure = {
                    error.value = when (it.kind) {
                        JoinRule.Private, JoinRule.Restricted -> i18n.searchGroupJoinFailedIsPrivate()
                        JoinRule.Invite -> i18n.searchGroupJoinFailedRequiresInvite()
                        is JoinRule.Unknown -> {
                            log.warn { "Encountered Unknown join rule (${it.kind.name}) for room (${searchGroup.roomId})" }
                            i18n.searchGroupJoinFailedGeneric()
                        }

                        else -> {
                            log.warn { "This should never happen as you can always join a room with a Join, Knock or KnockRestricted rule" }
                            null
                        }
                    }
                },
                onError = {
                    error.value = when (it.kind) {
                        JoinRule.Private, JoinRule.Restricted -> {
                            log.error(it.error) { "Determining whether or not to join (${searchGroup.roomId}) failed" }
                            i18n.searchGroupJoinFailedGeneric()
                        }

                        JoinRule.Invite -> {
                            log.error(it.error) { "Determining whether or not to join (${searchGroup.roomId}) failed" }
                            i18n.searchGroupJoinFailedGeneric()
                        }

                        JoinRule.Public -> {
                            log.error(it.error) { "cannot join room (${searchGroup.roomId})" }
                            i18n.searchGroupJoinFailedGeneric()
                        }

                        JoinRule.Knock -> {
                            log.error(it.error) { "cannot knock room (${searchGroup.roomId}) ${if (reason == null) "" else "with reason $reason"}" }
                            i18n.searchGroupJoinFailedGeneric()
                        }

                        JoinRule.KnockRestricted -> {
                            log.error(it.error) { "Determining whether or not to join (${searchGroup.roomId}) failed" }
                            i18n.searchGroupJoinFailedGeneric()
                        }


                        is JoinRule.Unknown -> {
                            log.error(it.error) { "Unknown join rule (${it.kind.name}) for room (${searchGroup.roomId}) failed" }
                            i18n.searchGroupJoinFailedGeneric()
                        }
                    }
                },
            )
        }.invokeOnCompletion { addGroupInProgress.value = false }
    }

    override fun back() {
        onBack()
    }

}
