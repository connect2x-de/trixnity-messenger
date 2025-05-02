package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val foundUsers: MutableStateFlow<List<Search.SearchUserElement>>
    val waitForUserResults: StateFlow<Boolean>
}

@OptIn(FlowPreview::class)
class DefaultUserSearchHandler(
    coroutineScope: CoroutineScope,
    private val search: Search,
    private val client: MatrixClient,
    private val debounceDuration: Duration = 300.toDuration(DurationUnit.MILLISECONDS),
    private val limit: Long? = 100,
    private val maxAvatarSize: Long,
    private val filterNot: (UserId) -> Boolean = { false },
    private val skippedUsers: StateFlow<Set<UserId>> = MutableStateFlow(emptySet()),
) : UserSearchHandler {
    override val searchTerm = TextFieldViewModelImpl()
    override val initialUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val foundUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val waitForUserResults: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        coroutineScope.launch { searchUsers() }
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
                    foundUsers.value = initialUsers.value
                } else {
                    waitForUserResults.value = true
                    skippedUsers.collectLatest { skippedUser ->
                        foundUsers.value = search.searchUsers(
                            client,
                            it,
                            limit,
                            { filterNot(it) || skippedUser.contains(it) },
                            this,
                            maxAvatarSize
                        )
                        waitForUserResults.value = false
                    }
                }
            }
    }
}

object PreviewUserSearchHandler : UserSearchHandler {
    override val searchTerm = TextFieldViewModelImpl("bla")
    override val initialUsers: StateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val foundUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val waitForUserResults: StateFlow<Boolean> = MutableStateFlow(false)
}
