package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import korlibs.io.async.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface UserSearchHandler {
    val searchTerm: MutableStateFlow<String>
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
    private val filterNot: (UserId) -> Boolean = { false }
) : UserSearchHandler {
    companion object {
        // Pattern that matches MXIDs without case sensitivity
        private val mxidPattern: Regex =
            Regex("""${UserId.sigilCharacter}([a-zA-Z\d.\-_=/]+):(${MatrixRegex.domain.pattern})""")
    }

    override val searchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val initialUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val foundUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val waitForUserResults: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        coroutineScope.launch(::searchUsers)
    }

    private suspend fun searchUsers() {
        searchTerm
            .onEach { if (it.isBlank()) foundUsers.value = initialUsers.value }
            .debounce(debounceDuration)
            .filter { it.isNotBlank() }
            .map {
                if (mxidPattern.matches(it)) it.lowercase()
                else it
            }
            .scopedCollectLatest {
                waitForUserResults.value = true
                foundUsers.value = search.searchUsers(client, it, limit, filterNot, this)
                waitForUserResults.value = false
            }
    }
}

object PreviewUserSearchHandler : UserSearchHandler {
    override val searchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val initialUsers: StateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val foundUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val waitForUserResults: StateFlow<Boolean> = MutableStateFlow(false)
}
