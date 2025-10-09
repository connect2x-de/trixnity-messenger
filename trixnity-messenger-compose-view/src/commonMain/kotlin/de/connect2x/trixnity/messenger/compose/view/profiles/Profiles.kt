package de.connect2x.trixnity.messenger.compose.view.profiles

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerProfileSettings
import de.connect2x.trixnity.messenger.multi.ProfileCreationViewModelImpl
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext


interface ProfilesView {
    @Composable
    fun create(
        matrixMultiMessenger: MatrixMultiMessenger,
        existingProfiles: Map<String, MatrixMultiMessengerProfileSettings>,
    )
}

@Composable
fun Profiles(
    matrixMultiMessenger: MatrixMultiMessenger,
    existingProfiles: Map<String, MatrixMultiMessengerProfileSettings>,
) {
    DI.get<ProfilesView>().create(matrixMultiMessenger, existingProfiles)
}

class ProfilesViewImpl : ProfilesView {
    @Composable
    override fun create(
        matrixMultiMessenger: MatrixMultiMessenger,
        existingProfiles: Map<String, MatrixMultiMessengerProfileSettings>,
    ) {
        val multiProfile = DI.get<MatrixMultiMessengerConfiguration>().multiProfile
        Surface(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            if (existingProfiles.isEmpty() || !multiProfile) {
                createAndUseDefaultUserProfile(matrixMultiMessenger)
            } else {
                createOrSelectManualUserProfile(
                    matrixMultiMessenger,
                    existingProfiles
                )
            }
        }
    }
}

@Composable
fun createOrSelectManualUserProfile(
    matrixMultiMessenger: MatrixMultiMessenger,
    existingProfiles: Map<String, MatrixMultiMessengerProfileSettings>,
) {
    val di = DI.current
    val coroutineScope = rememberCoroutineScope()
    val profileCreationViewModel = remember { ProfileCreationViewModelImpl(di, coroutineScope) }
    val showProfileCreation = ShowProfileCreation.current
    if (existingProfiles.isEmpty() || showProfileCreation.value) {
        ProfileCreation(profileCreationViewModel) {
            showProfileCreation.value = false
        }
    } else {
        ProfileSelection(matrixMultiMessenger)
    }
}

@Composable
fun createAndUseDefaultUserProfile(matrixMultiMessenger: MatrixMultiMessenger) {
    LaunchedEffect(Unit) {
        withContext(NonCancellable) {
            if (matrixMultiMessenger.activeProfile.value == null) {
                val profile = matrixMultiMessenger.profiles.value.keys.firstOrNull()
                    ?: matrixMultiMessenger.createProfile()
                matrixMultiMessenger.selectProfile(profile)
            }
        }
    }
}
