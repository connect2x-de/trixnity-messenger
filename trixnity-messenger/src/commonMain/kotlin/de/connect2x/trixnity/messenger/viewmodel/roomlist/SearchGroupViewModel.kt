package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.util.isKnock
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
    val foundGroups: StateFlow<List<SearchGroup>>
    val groupSearchInProgress: StateFlow<Boolean>
    val joinGroupInProgress: StateFlow<Boolean>
    val knockingGroupInProgress: StateFlow<Boolean>
    val knockGroupModalShown: StateFlow<Boolean>

    fun joinGroup(searchGroup: SearchGroup)
    fun knockGroup(searchGroup: SearchGroup, reason: String? = null)
    fun showKnockGroupModal()
    fun hideKnockGroupModal()
    fun back()

    data class SearchGroup(
        val roomId: RoomId,
        val groupName: String,
        val topic: String?,
        val image: StateFlow<ByteArray?>,
        val initials: String,
        val joinedMembersCount: Long,
        val isKnock: Boolean
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

    override val searchTerm = TextFieldViewModelImpl()
    override val groupSearchInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val joinGroupInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val knockingGroupInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val knockGroupModalShown: MutableStateFlow<Boolean> = MutableStateFlow(false)

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
                        // TODO report error
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
                                isKnock = publicRoomsChunk.joinRule.isKnock
                            )
                        }
                    }
                )
            } finally {
                groupSearchInProgress.update { false }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun joinGroup(searchGroup: SearchGroupViewModel.SearchGroup) {
        if (searchGroup.isKnock) {
            log.warn { "Cannot join knock room (${searchGroup.roomId})" }
            return
        }

        if (joinGroupInProgress.getAndUpdate { true }) {
            log.warn { "Joining room (${searchGroup.roomId}) is already in progress" }
            return
        }

        coroutineScope.launch {
            matrixClient.api.room.joinRoom(searchGroup.roomId).fold(
                onFailure = {
                    log.error(it) { "cannot join room (${searchGroup.roomId})" }
                    // TODO error msg
                },
                onSuccess = {
                    onGroupJoined(userId, searchGroup.roomId)
                }
            )
        }.invokeOnCompletion { joinGroupInProgress.value = false }
    }

    override fun knockGroup(searchGroup: SearchGroupViewModel.SearchGroup, reason: String?) {
        if (!searchGroup.isKnock) {
            log.warn { "Cannot knock non-knock room (${searchGroup.roomId})" }
            return
        }

        if (knockingGroupInProgress.getAndUpdate { true }) {
            log.warn { "Knocking room (${searchGroup.roomId}) is already in progress" }
            return
        }

        coroutineScope.launch {
            matrixClient.api.room.knockRoom(roomId = searchGroup.roomId, reason = reason).fold(
                onFailure = {
                    log.error(it) { "cannot knock room (${searchGroup.roomId}) ${if (reason == null) "" else "with reason $reason"}" }
                    // TODO error msg
                },
                onSuccess = {
                    onGroupKnocked(searchGroup.roomId)
                }
            )
        }.invokeOnCompletion { knockingGroupInProgress.value = false }
    }

    override fun showKnockGroupModal() {
        knockGroupModalShown.value = true
    }

    override fun hideKnockGroupModal() {
        knockGroupModalShown.value = false
    }

    override fun back() {
        onBack()
    }

}
