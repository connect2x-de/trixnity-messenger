package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface UserSearchHandler {
    val searchTerm: TextFieldViewModel
    val initialUsers: StateFlow<List<Search.SearchUserElement>>
    val selectedUsers: StateFlow<List<Search.SearchUserElement>>
    val foundUsers: StateFlow<List<Search.SearchUserElement>>
    val waitForUserResults: StateFlow<Boolean>

    fun selectUser(user: Search.SearchUserElement)
    fun unselectUser(user: Search.SearchUserElement)
}

@OptIn(FlowPreview::class)
class DefaultUserSearchHandler(
    coroutineScope: CoroutineScope,
    private val search: Search,
    private val client: MatrixClient,
    private val debounceDuration: Duration = 300.toDuration(DurationUnit.MILLISECONDS),
    private val limit: Long? = 100,
    private val maxAvatarSize: Long,
    filterNotUsers: Flow<Set<UserId>> = flowOf(emptySet()),
) : UserSearchHandler {
    override val searchTerm = TextFieldViewModelImpl()
    override val initialUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val selectedUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    private val unfilteredFoundUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val foundUsers: StateFlow<List<Search.SearchUserElement>> =
        combine(unfilteredFoundUsers, selectedUsers, filterNotUsers) { foundUsers, selectedUsers, filterNotUsers ->
            foundUsers
                .filterNot(selectedUsers::contains)
                .filterNot { filterNotUsers.contains(it.userId) }
        }.stateIn(coroutineScope, WhileSubscribed(), emptyList())
    override val waitForUserResults: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        coroutineScope.launch { searchUsers() }
    }

    override fun selectUser(user: Search.SearchUserElement) {
        selectedUsers.value += user
    }

    override fun unselectUser(user: Search.SearchUserElement) {
        selectedUsers.value -= user
    }

    private suspend fun searchUsers() {
        searchTerm
            .map { it.text }
            .distinctUntilChanged()
            .debounce(debounceDuration)
            .map {
                if (MatrixRegex.userId.matches(it.lowercase())) it.lowercase()
                else it
            }
            .scopedCollectLatest {
                if (it.isBlank()) {
                    unfilteredFoundUsers.value = initialUsers.value
                } else {
                    waitForUserResults.value = true
                    unfilteredFoundUsers.value =
                        search.searchUsers(
                            client,
                            it,
                            limit,
                            this,
                            maxAvatarSize
                        )
                    waitForUserResults.value = false
                }
            }
    }
}


object PreviewUserSearchHandler : UserSearchHandler {
    override val searchTerm = TextFieldViewModelImpl("bla")
    override val initialUsers: StateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val foundUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val selectedUsers: StateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val waitForUserResults: StateFlow<Boolean> = MutableStateFlow(false)
    override fun selectUser(user: Search.SearchUserElement) {}
    override fun unselectUser(user: Search.SearchUserElement) {}
}
