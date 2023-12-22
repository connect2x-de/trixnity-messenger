package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
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
    ): SearchGroupViewModel =
        SearchGroupViewModelImpl(viewModelContext, onBack, onGroupJoined)

    companion object : SearchGroupViewModelFactory
}

interface SearchGroupViewModel {
    val searchTerm: MutableStateFlow<String>
    val foundGroups: StateFlow<List<SearchGroup>>
    val groupSearchInProgress: StateFlow<Boolean>
    val joinGroupInProgress: StateFlow<Boolean>

    fun joinGroup(roomId: RoomId)
    fun back()

    data class SearchGroup(
        val roomId: RoomId,
        val groupName: String,
        val topic: String?,
        val image: StateFlow<ByteArray?>,
        val initials: String,
        val joinedMembersCount: Long,
    )
}

@OptIn(FlowPreview::class)
open class SearchGroupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onBack: () -> Unit,
    private val onGroupJoined: (UserId, RoomId) -> Unit,
) : SearchGroupViewModel, MatrixClientViewModelContext by viewModelContext {

    private val initials = get<Initials>()

    override val searchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val groupSearchInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val joinGroupInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val foundGroups: StateFlow<List<SearchGroupViewModel.SearchGroup>> = searchTerm
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
                            )
                        }
                    }
                )
            } finally {
                groupSearchInProgress.update { false }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun joinGroup(roomId: RoomId) {
        coroutineScope.launch {
            joinGroupInProgress.update { true }
            try {
                matrixClient.api.room.joinRoom(roomId)
                    .onFailure {
                        log.error(it) { "cannot join room" }
                        // TODO error msg
                    }
                    .onSuccess {
                        onGroupJoined(userId, roomId)
                    }
            } finally {
                joinGroupInProgress.update { false }
            }
        }
    }

    override fun back() {
        onBack()
    }

}