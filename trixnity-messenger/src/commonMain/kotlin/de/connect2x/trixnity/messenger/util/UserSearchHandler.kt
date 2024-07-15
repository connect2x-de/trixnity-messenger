package de.connect2x.trixnity.messenger.util

import korlibs.io.async.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface UserSearchHandler {
    val initialUsers: StateFlow<List<Search.SearchUserElement>>
    val searchTerm: StateFlow<String>
    val foundUsers: StateFlow<List<Search.SearchUserElement>>
    val waitForUserResults: StateFlow<Boolean>

    fun setSearchTerm(value: String)
    fun addFoundUserElement(element: Search.SearchUserElement)
    fun removeFoundUserElement(element: Search.SearchUserElement)
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DefaultUserSearchHandler(
    coroutineScope: CoroutineScope,
    private val search: Search,
    private val client: MatrixClient,
    private val debounceDuration: Duration = 300.toDuration(DurationUnit.MILLISECONDS),
    private val limit: Long? = 100,
    private val filterNot: (UserId) -> Boolean = { true }
) : UserSearchHandler {
    companion object {
        // Pattern that matches out-of-spec MXID expressions for correcting them
        private val mxidPattern: Regex =
            Regex("""${UserId.sigilCharacter}([a-zA-Z\d.\-_=/]+):(${MatrixRegex.domain.pattern})""")
    }

    override val initialUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val searchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val foundUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val waitForUserResults: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        coroutineScope.launch(::searchUsers)
    }

    private suspend fun searchUsers() {
        searchTerm
            .mapLatest {
                if (it.isBlank()) foundUsers.value = initialUsers.value
                if (mxidPattern.matches(it)) it.lowercase()
                else it
            }
            .debounce(debounceDuration)
            .filter { it.isNotBlank() }
            .collect {
                waitForUserResults.value = true
                foundUsers.value = search.searchUsers(client, it, limit, filterNot)
                waitForUserResults.value = false
            }
    }

    override fun setSearchTerm(value: String) {
        searchTerm.value = value
    }

    override fun addFoundUserElement(element: Search.SearchUserElement) {
        foundUsers.value += element
    }

    override fun removeFoundUserElement(element: Search.SearchUserElement) {
        foundUsers.value -= element
    }
}

object PreviewUserSearchHandler : UserSearchHandler {
    override val initialUsers: StateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val searchTerm: StateFlow<String> = MutableStateFlow("")
    override val foundUsers: StateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val waitForUserResults: StateFlow<Boolean> = MutableStateFlow(false)

    override fun setSearchTerm(value: String) {}
    override fun addFoundUserElement(element: Search.SearchUserElement) {}
    override fun removeFoundUserElement(element: Search.SearchUserElement) {}
}
