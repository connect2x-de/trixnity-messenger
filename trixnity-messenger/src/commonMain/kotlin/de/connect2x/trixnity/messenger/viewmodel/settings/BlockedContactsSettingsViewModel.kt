package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.component.get

interface BlockedContactsSettingsViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onCloseBlockedContactsSettings: () -> Unit,
    ): BlockedContactsSettingsViewModel =
        BlockedContactsSettingsViewModelImpl(
            viewModelContext,
            onCloseBlockedContactsSettings = onCloseBlockedContactsSettings,
        )

    companion object : BlockedContactsSettingsViewModelFactory
}

interface BlockedContactsSettingsViewModel {
    val account: UserId
    val blockedContactsCount: StateFlow<Int>
    val blockedContactsList: StateFlow<List<BlockedContact>>
    fun unblockContact(userId: UserId)
    fun back()
}

class BlockedContactsSettingsViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onCloseBlockedContactsSettings: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext,
    BlockedContactsSettingsViewModel {

    override val account = viewModelContext.userId

    private val backCallback = BackCallback {
        back()
    }

    private val userBlocking = get<UserBlocking>()

    private val blockedUserIdsList: StateFlow<List<UserId>> =
        userBlocking.getBlockedUsers(matrixClient)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

    override val blockedContactsCount: StateFlow<Int> =
        blockedUserIdsList.map { it.count() }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), 0)

    private val isUnblockingContactList =
        MutableStateFlow<Set<UserId>>(setOf())

    override val blockedContactsList: StateFlow<List<BlockedContact>> = combine(
        isUnblockingContactList,
        blockedUserIdsList,
    ) { isUnignoringList, userIdList ->
        userIdList.map { userId ->
            BlockedContact(
                userId,
                isUnblocking = isUnignoringList.contains(userId),
            )
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

    override fun unblockContact(userId: UserId) {
        if (isUnblockingContactList.value.contains(userId)) {
            log.error { "user is already being unblocked" }
            return
        }
        coroutineScope.launch {
            isUnblockingContactList.value += userId
            try {
                userBlocking.unblockUser(
                    matrixClient, userId,
                    onFailure = {
                        log.error(it) { "failed to unblock contact" }
                    },
                    onSuccess = {
                        log.debug { "unblocked user" }
                    },
                )
            } finally {
                isUnblockingContactList.value -= userId
            }
        }
    }

    override fun back() {
        onCloseBlockedContactsSettings()
    }

    init {
        registerBackCallback(backCallback)
    }
}

data class BlockedContact(
    val userId: UserId,
    val isUnblocking: Boolean,
)
