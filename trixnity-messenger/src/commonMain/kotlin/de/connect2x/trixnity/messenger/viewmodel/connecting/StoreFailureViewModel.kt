package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.StoreAccessException
import de.connect2x.trixnity.messenger.StoreLockedException
import de.connect2x.trixnity.messenger.closeApp
import de.connect2x.trixnity.messenger.deleteDatabase
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface StoreFailureViewModelFactory {
    fun newStoreFailureViewModel(
        viewModelContext: ViewModelContext,
        accountName: String,
        storeFailure: Result<Unit>?,
    ): StoreFailureViewModel {
        return StoreFailureViewModelImpl(viewModelContext, accountName, storeFailure)
    }
}

interface StoreFailureViewModel {
    val closeApplicationFlow: StateFlow<Boolean>
    val deleteDbOrCloseApplication: StateFlow<Boolean>
    fun closeApplication()
    fun deleteDb()
}

open class StoreFailureViewModelImpl(
    viewModelContext: ViewModelContext,
    private val _accountName: String,
    storeFailure: Result<Unit>?,
) : ViewModelContext by viewModelContext, StoreFailureViewModel {

    override val closeApplicationFlow =
        MutableStateFlow(storeFailure?.exceptionOrNull() is StoreLockedException).asStateFlow()
    override val deleteDbOrCloseApplication =
        MutableStateFlow(storeFailure?.exceptionOrNull() is StoreAccessException).asStateFlow()

    override fun closeApplication() {
        closeApp()
    }

    override fun deleteDb() {
        deleteDatabase(_accountName)
    }
}