package de.connect2x.trixnity.messenger.compose.view.profiles

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.multi.ProfileCreationViewModelFactory
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.multi.singleModeMatrixMessenger
import kotlinx.coroutines.flow.first


interface ProfilesView {
    @Composable
    fun create()
}

@Composable
fun Profiles() {
    DI.get<ProfilesView>().create()
}

class ProfilesViewImpl : ProfilesView {
    @Composable
    override fun create() {
        val profileManager = DI.get<ProfileManager>()
        val multiProfile = profileManager.isMultiProfileEnabled.collectAsState().value
        val existingProfiles = profileManager.profiles.collectAsState().value
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (existingProfiles.isEmpty() || multiProfile == false) {
                createAndUseDefaultUserProfile()
            } else {
                createOrSelectManualUserProfile()
            }
        }
    }
}

@Composable
fun createOrSelectManualUserProfile() {
    val di = DI.current
    val profileManager = DI.get<ProfileManager>()
    val coroutineScope = rememberCoroutineScope()
    val profileCreationViewModel = remember { di.get<ProfileCreationViewModelFactory>().create(di, coroutineScope) }
    val showProfileCreation = ShowProfileCreation.current
    val existingProfiles = profileManager.profiles.collectAsState().value
    if (existingProfiles.isEmpty() || showProfileCreation.value) {
        ProfileCreation(
            textFieldViewModel = profileCreationViewModel.profileName,
            error = profileCreationViewModel.error.collectAsState().value,
            onFinish = { showProfileCreation.value = false },
            onCreate = { profileCreationViewModel.createProfile() },
            canCreateProfile = profileCreationViewModel.canCreateProfile.collectAsState().value
        )
    } else {
        ProfileSelection(profileManager)
    }
}

@Composable
fun createAndUseDefaultUserProfile() {
    val pm = DI.get<ProfileManager>()
    LaunchedEffect(Unit) { pm.singleModeMatrixMessenger().first() }
}
