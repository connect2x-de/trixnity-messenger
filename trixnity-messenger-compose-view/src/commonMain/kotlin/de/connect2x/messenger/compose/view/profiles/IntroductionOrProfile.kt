package de.connect2x.messenger.compose.view.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.multi.ProfileManager

@Composable
fun IntroductionOrProfile() = DI.get<IntroductionOrProfileView>().create()

interface IntroductionOrProfileView {
    @Composable
    fun create()
}

class IntroductionOrProfileViewImpl : IntroductionOrProfileView {
    @Composable
    override fun create() {
        val pm = DI.get<ProfileManager>()
        val multiProfileConfig = pm.isMultiProfileEnabled.collectAsState().value
        val open = remember { mutableStateOf(multiProfileConfig == null) }
        if (multiProfileConfig == null || open.value) {
            Introduction(open)
        } else {
            Profiles()
        }
    }
}
