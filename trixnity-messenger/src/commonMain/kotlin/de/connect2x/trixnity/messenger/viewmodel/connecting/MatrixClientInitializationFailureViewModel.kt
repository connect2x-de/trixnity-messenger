package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClientInitializationException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface MatrixClientInitializationFailureViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        userId: UserId,
        exception: MatrixClientInitializationException,
        onDeletionFinished: () -> Unit,
    ): MatrixClientInitializationFailureViewModel {
        return MatrixClientInitializationFailureViewModelImpl(viewModelContext, userId, exception, onDeletionFinished)
    }

    companion object : MatrixClientInitializationFailureViewModelFactory
}

interface MatrixClientInitializationFailureViewModel {
    val userId: UserId
    val deleteEnabled: Boolean

    fun closeApplication()

    fun delete()
}

open class MatrixClientInitializationFailureViewModelImpl(
    viewModelContext: ViewModelContext,
    override val userId: UserId,
    exception: MatrixClientInitializationException,
    private val onDeletionFinished: () -> Unit,
) : ViewModelContext by viewModelContext, MatrixClientInitializationFailureViewModel {

    override val deleteEnabled = exception !is MatrixClientInitializationException.DatabaseLockedException

    override fun closeApplication() {
        getOrNull<CloseApp>()?.invoke()
    }

    val matrixClients = get<MatrixClients>()

    override fun delete() {
        coroutineScope.launch {
            matrixClients.remove(userId)
            onDeletionFinished()
        }
    }
}
