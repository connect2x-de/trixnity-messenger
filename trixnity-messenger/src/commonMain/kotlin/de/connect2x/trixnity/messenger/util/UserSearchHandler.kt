package de.connect2x.trixnity.messenger.util

import korlibs.io.async.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface UserSearchHandler {
    val initialUsers: MutableStateFlow<List<Search.SearchUserElement>>
    val currentSearchTerm: MutableStateFlow<String>
    val actualSearchTerm: MutableStateFlow<String>
    val foundUsers: MutableStateFlow<List<Search.SearchUserElement>>
    val waitForUserResults: MutableStateFlow<Boolean>

    fun setSearchTerm(value: String)
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
    override val currentSearchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val actualSearchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val foundUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val waitForUserResults: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        coroutineScope.launch(::updateSearchTerm)
        coroutineScope.launch(::searchUsers)
    }

    private suspend fun searchUsers() {
        actualSearchTerm
            .onEach { if (it.isBlank()) foundUsers.value = initialUsers.value }
            .filter { it.isNotBlank() }
            .collect {
                waitForUserResults.value = true
                foundUsers.value = search.searchUsers(client, it, limit, filterNot)
                waitForUserResults.value = false
            }
    }

    private suspend fun updateSearchTerm() {
        currentSearchTerm
            .debounce(debounceDuration)
            .mapLatest {
                if (mxidPattern.matches(it)) it.lowercase()
                else it
            }
            .collect { actualSearchTerm.value = it }
    }

    override fun setSearchTerm(value: String) {
        currentSearchTerm.value = value
        actualSearchTerm.value = value
    }
}

object PreviewUserSearchHandler : UserSearchHandler {
    override val initialUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val currentSearchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val actualSearchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val foundUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    override val waitForUserResults: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun setSearchTerm(value: String) {}
}
