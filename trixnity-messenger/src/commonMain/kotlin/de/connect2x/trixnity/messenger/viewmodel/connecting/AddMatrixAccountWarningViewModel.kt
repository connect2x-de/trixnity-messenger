package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface AddMatrixAccountWarningViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCancel: () -> Unit,
        onCreateAccount: () -> Unit
    ): AddMatrixAccountWarningViewModel {
        return AddMatrixAccountWarningViewModelImpl(viewModelContext, onCancel, onCreateAccount)
    }

    companion object : AddMatrixAccountWarningViewModelFactory
}

interface AddMatrixAccountWarningViewModel {
    val isMultiProfile: StateFlow<Boolean>
    fun cancelWarning()

    fun createAccount()

    fun logoutFromProfile()
}

class AddMatrixAccountWarningViewModelImpl(
    viewModelContext: ViewModelContext,
    val onCancel: () -> Unit,
    val onCreateAccount: () -> Unit
) : AddMatrixAccountWarningViewModel,
    ViewModelContext by viewModelContext {
    val profileManager = getOrNull<ProfileManager>()
    override val isMultiProfile: StateFlow<Boolean> =
        (profileManager?.isMultiProfileEnabled?.map { it == true } ?: flowOf(false)).stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(),
            false
        )

    override fun logoutFromProfile() {
        coroutineScope.launch {
            profileManager?.closeProfile()
        }
    }

    override fun cancelWarning() {
        onCancel()
    }

    override fun createAccount() {
        onCreateAccount()
    }
}
