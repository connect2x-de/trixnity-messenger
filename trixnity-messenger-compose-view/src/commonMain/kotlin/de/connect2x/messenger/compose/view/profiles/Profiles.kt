package de.connect2x.messenger.compose.view.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerProfileSettings
import de.connect2x.trixnity.messenger.multi.ProfileCreationViewModelImpl
import de.connect2x.trixnity.messenger.multi.singleModeMatrixMessenger
import kotlinx.coroutines.flow.first


interface ProfilesView {
    @Composable
    fun create(
        matrixMultiMessenger: MatrixMultiMessenger,
        existingProfiles: Map<String, MatrixMultiMessengerProfileSettings>,
        onCancel: () -> Unit,
    )
}

@Composable
fun Profiles(
    matrixMultiMessenger: MatrixMultiMessenger,
    existingProfiles: Map<String, MatrixMultiMessengerProfileSettings>,
    onCancel: () -> Unit,
) {
    DI.get<ProfilesView>().create(matrixMultiMessenger, existingProfiles, onCancel)
}

class ProfilesViewImpl : ProfilesView {
    @Composable
    override fun create(
        matrixMultiMessenger: MatrixMultiMessenger,
        existingProfiles: Map<String, MatrixMultiMessengerProfileSettings>,
        onCancel: () -> Unit,
    ) {
        if (existingProfiles.isEmpty()) {
            createAndUseDefaultUserProfile(matrixMultiMessenger)
        } else {
            createOrSelectManualUserProfile(
                matrixMultiMessenger,
                existingProfiles,
                onCancel,
            )
        }
    }
}

@Composable
fun createOrSelectManualUserProfile(
    matrixMultiMessenger: MatrixMultiMessenger,
    existingProfiles: Map<String, MatrixMultiMessengerProfileSettings>,
    onCancel: () -> Unit,
) {
    val di = DI.current
    val coroutineScope = rememberCoroutineScope()
    val profileCreationViewModel = remember { ProfileCreationViewModelImpl(di, coroutineScope) }
    val showProfileCreation = ShowProfileCreation.current
    if (existingProfiles.isEmpty() || showProfileCreation.value) {
        ProfileCreation(profileCreationViewModel)
    } else {
        ProfileSelection(matrixMultiMessenger, onCancel = onCancel)
    }
}

@Composable
fun createAndUseDefaultUserProfile(matrixMultiMessenger: MatrixMultiMessenger) {
    LaunchedEffect(Unit) {
        matrixMultiMessenger.singleModeMatrixMessenger().first()
    }
}
