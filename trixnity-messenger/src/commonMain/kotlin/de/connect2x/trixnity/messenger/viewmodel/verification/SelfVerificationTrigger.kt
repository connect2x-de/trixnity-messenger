package de.connect2x.trixnity.messenger.viewmodel.verification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.folivo.trixnity.core.model.UserId


interface SelfVerificationTrigger {
    val onInvoke: Flow<UserId>
    suspend fun invoke(userId: UserId)
}

class SelfVerificationTriggerImpl : SelfVerificationTrigger {
    override val onInvoke = MutableSharedFlow<UserId>()
    override suspend fun invoke(userId: UserId) {
        onInvoke.emit(userId)
    }
}
