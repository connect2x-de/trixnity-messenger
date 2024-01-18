package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.LoadStoreException
import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.util.DeleteAccountData
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.inject

interface StoreFailureViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        userId: UserId,
        exception: LoadStoreException,
    ): StoreFailureViewModel {
        return StoreFailureViewModelImpl(viewModelContext, userId, exception)
    }

    companion object : StoreFailureViewModelFactory
}

interface StoreFailureViewModel {
    val deleteEnabled: Boolean
    fun closeApplication()
    fun deleteDb()
}

open class StoreFailureViewModelImpl(
    viewModelContext: ViewModelContext,
    private val userId: UserId,
    exception: LoadStoreException,
) : ViewModelContext by viewModelContext, StoreFailureViewModel {

    override val deleteEnabled = exception is LoadStoreException.StoreAccessException

    override fun closeApplication() {
        getOrNull<CloseApp>()?.invoke()
    }

    private val deleteAccountData by inject<DeleteAccountData>()

    override fun deleteDb() {
        coroutineScope.launch {
            deleteAccountData(userId)
        }
    }
}