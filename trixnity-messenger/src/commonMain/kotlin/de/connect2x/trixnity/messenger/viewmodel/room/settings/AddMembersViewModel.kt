package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.RoomId

private val log = KotlinLogging.logger {}

interface AddMembersViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        addMembersToRoomViewModel: PotentialMembersViewModel,
        onBack: () -> Unit,
    ): AddMembersViewModel {
        return AddMembersViewModelImpl(
            viewModelContext,
            roomId,
            addMembersToRoomViewModel,
            onBack,
        )
    }

    companion object : AddMembersViewModelFactory
}

interface AddMembersViewModel {
    val potentialMembersViewModel: PotentialMembersViewModel
    val groupUsers: MutableStateFlow<List<Search.SearchUserElement>>
    val canAddMembers: StateFlow<Boolean>
    val error: StateFlow<String?>
    val errorCause: StateFlow<String?>

    fun onUserClick(user: Search.SearchUserElement)
    fun addMembers()
    fun errorDismiss()
    fun back()

    // IMPORTANT: has to be separate as the renderer will collapse when 2 collectAsState() references change at the same time
    fun removeUserFromList(user: Search.SearchUserElement)
    fun removeUserFromGroup(user: Search.SearchUserElement)
    fun addUserToList(user: Search.SearchUserElement)
}

class AddMembersViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    override val potentialMembersViewModel: PotentialMembersViewModel,
    private val onBack: () -> Unit,
) : AddMembersViewModel, MatrixClientViewModelContext by viewModelContext {

    private val backCallback = BackCallback {
        onBack()
    }

    init {
        backHandler.register(backCallback)
    }

    override fun back() {
        onBack()
    }

    override val groupUsers = MutableStateFlow(listOf<Search.SearchUserElement>())
    override val canAddMembers =
        groupUsers.map { it.isNotEmpty() }
            .stateIn(coroutineScope, started = SharingStarted.WhileSubscribed(), false)

    override val error = MutableStateFlow<String?>(null)
    override val errorCause = MutableStateFlow<String?>(null)
    internal val foundUsers = potentialMembersViewModel.searchHandler.foundUsers

    override fun addMembers() {
        log.info { "add ${groupUsers.value.joinToString { it.displayName }} to group" }
        coroutineScope.launch {
            val failedInvitations = mutableListOf<Pair<Search.SearchUserElement, Throwable>>()
            for (user in groupUsers.value) {
                matrixClient.api.room.inviteUser(roomId, user.userId)
                    .fold(onSuccess = {
                        log.debug { "user ${user.userId.full} was invited" }

                    },
                        onFailure = {
                            log.error(it) { "Failed to invite user ${user.userId.full}" }
                            log.error { it.stackTraceToString() }
                            failedInvitations.add(user to it)
                        })

            }
            when (failedInvitations.count()) {
                0 -> onBack()

                1 -> {
                    val throwable = failedInvitations.first().second
                    error.value = i18n.settingsRoomAddMembersErrorSingular(failedInvitations.first().first.displayName)
                    errorCause.value = when {
                        potentialMembersViewModel.offline.value ->
                            i18n.settingsRoomAddMembersErrorOffline()

                        throwable.message != null -> throwable.message
                        else -> throwable.stackTraceToString().lines().first()
                    }
                }

                else -> {
                    val throwable = failedInvitations.first().second

                    error.value =
                        i18n.settingsRoomAddMembersErrorPlural(
                            failedInvitations.joinTo(
                                StringBuilder(),
                                limit = failedInvitations.size - 1,
                                suffix = " " +
                                        i18n.settingsRoomAddMembersAnd()
                                        + " \"" + failedInvitations.last().first.displayName + "\""
                            ) { "\"" + it.first.displayName + "\"" }.toString()
                        )
                    errorCause.value = when {
                        potentialMembersViewModel.offline.value ->
                            i18n.settingsRoomAddMembersErrorOffline()

                        throwable.message != null -> throwable.message
                        else -> throwable.stackTraceToString().lines().first()
                    }
                }
            }
        }
    }

    override fun errorDismiss() {
        error.value = null
    }

    override fun onUserClick(user: Search.SearchUserElement) {
        if (groupUsers.value.contains(user).not()) {
            groupUsers.value += user
            removeUserFromList(user)
        }
    }

    // IMPORTANT: has to be separate as the renderer will collapse when 2 collectAsState() references change at the same time
    override fun removeUserFromList(user: Search.SearchUserElement) {
        coroutineScope.launch {
            delay(50)
            potentialMembersViewModel.searchHandler.foundUsers.value -= user
        }
    }

    override fun removeUserFromGroup(user: Search.SearchUserElement) {
        groupUsers.value -= user
        addUserToList(user)
    }

    override fun addUserToList(user: Search.SearchUserElement) {
        coroutineScope.launch {
            delay(50)
            potentialMembersViewModel.searchHandler.foundUsers.value += user
        }
    }

    private fun <T, A : Appendable> Iterable<T>.joinTo(
        buffer: A,
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        suffix: CharSequence = "",
        limit: Int = -1,
        truncated: CharSequence = "",
        transform: ((T) -> CharSequence)? = null
    ): A {
        buffer.append(prefix)
        var count = 0
        for (element in this) {
            if (++count > 1 && count <= limit) buffer.append(separator)
            if (limit < 0 || count <= limit) {
                when {
                    transform != null -> buffer.append(transform(element))
                    element is CharSequence? -> buffer.append(element)
                    element is Char -> buffer.append(element)
                    else -> buffer.append(element.toString())
                }
            } else break
        }
        if (limit in 0 until count) buffer.append(truncated)
        buffer.append(suffix)
        return buffer
    }
}
