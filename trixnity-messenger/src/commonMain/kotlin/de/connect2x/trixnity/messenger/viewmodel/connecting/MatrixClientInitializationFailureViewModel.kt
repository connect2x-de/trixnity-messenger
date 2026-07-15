package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClientInitializationException
import de.connect2x.trixnity.messenger.MatrixClientInitializationException.DatabaseAccessException
import de.connect2x.trixnity.messenger.MatrixClientInitializationException.DatabaseCannotBeDecryptedException
import de.connect2x.trixnity.messenger.MatrixClientInitializationException.DatabaseKeysManipulatedException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface MatrixClientInitializationFailureViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        userId: UserId,
        initializationException: MatrixClientInitializationException,
        onDeletionFinished: () -> Unit,
    ): MatrixClientInitializationFailureViewModel {
        return MatrixClientInitializationFailureViewModelImpl(
            viewModelContext,
            userId,
            initializationException,
            onDeletionFinished,
        )
    }

    companion object : MatrixClientInitializationFailureViewModelFactory
}

interface MatrixClientInitializationFailureViewModel {
    val userId: UserId
    val deleteEnabled: Boolean
    val initializationException: MatrixClientInitializationException?

    fun closeApplication()

    fun delete()

    /** Confirms that the deletion of the account or profile was successful. */
    fun confirmDeletion()
}

open class MatrixClientInitializationFailureViewModelImpl(
    viewModelContext: ViewModelContext,
    override val userId: UserId,
    override val initializationException: MatrixClientInitializationException,
    private val onDeletionFinished: () -> Unit,
) : ViewModelContext by viewModelContext, MatrixClientInitializationFailureViewModel {

    override val deleteEnabled = initializationException is DatabaseAccessException

    val matrixClients = get<MatrixClients>()
    val messengerConfiguration = get<MatrixMessengerConfiguration>()
    val profileManager = get<ProfileManager>()
    var deleteProfile = initializationException is DatabaseKeysManipulatedException

    init {
        when (initializationException) {
            is DatabaseKeysManipulatedException -> {
                log.error { "The keys for the database have been tampered with. Will delete the current profile." }
                // we cannot delete the profile immediately as it instantly removes this view, so we defer it until a
                // user interaction takes place [confirmDeletion].
            }

            is DatabaseCannotBeDecryptedException -> {
                log.error {
                    "The database cannot be decrypted. Will log out automatically and delete all local data for account."
                }
                deleteAccountAndWait()
                // UI should call confirmDeletion() afterwards
            }
            else -> {
                log.debug { "Found init exception: $initializationException" }
            }
        }
    }

    override fun closeApplication() {
        getOrNull<CloseApp>()?.invoke()
    }

    override fun delete() {
        coroutineScope.launch {
            matrixClients.remove(userId)
            onDeletionFinished()
        }
    }

    override fun confirmDeletion() {
        if (deleteProfile) {
            deleteProfile()
        }
        onDeletionFinished()
    }

    private fun deleteAccountAndWait() {
        coroutineScope.launch { matrixClients.remove(userId) }
    }

    private fun deleteProfile() {
        coroutineScope.launch {
            profileManager.activeProfile.value?.let { profile ->
                log.warn { "Deleting profile: $profile" }
                profileManager.deleteProfile(profile)
            } ?: log.warn { "Tried to delete profile, but no active profile found." }
        }
    }
}
