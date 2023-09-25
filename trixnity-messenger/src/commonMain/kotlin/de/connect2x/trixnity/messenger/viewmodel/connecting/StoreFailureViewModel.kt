package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.LoadStoreException
import de.connect2x.trixnity.messenger.closeApp
import de.connect2x.trixnity.messenger.deleteAccountDataLocally
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.launch

interface StoreFailureViewModelFactory {
    fun newStoreFailureViewModel(
        viewModelContext: ViewModelContext,
        accountName: String,
        exception: LoadStoreException,
    ): StoreFailureViewModel {
        return StoreFailureViewModelImpl(viewModelContext, accountName, exception)
    }
}

interface StoreFailureViewModel {
    val deleteEnabled: Boolean
    fun closeApplication()
    fun deleteDb()
}

open class StoreFailureViewModelImpl(
    viewModelContext: ViewModelContext,
    private val _accountName: String,
    exception: LoadStoreException,
) : ViewModelContext by viewModelContext, StoreFailureViewModel {

    override val deleteEnabled = exception is LoadStoreException.StoreAccessException

    override fun closeApplication() {
        closeApp()
    }

    override fun deleteDb() {
        coroutineScope.launch {
            deleteAccountDataLocally(_accountName)
        }
    }
}