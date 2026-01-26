package de.connect2x.trixnity.messenger.compose.view.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get

@Composable
fun IntroductionOrProfile() = DI.get<IntroductionOrProfileView>().create()

interface IntroductionOrProfileView {
    @Composable
    fun create()
}

class IntroductionOrProfileViewImpl : IntroductionOrProfileView {
    @Composable
    override fun create() {
        //There is currently no content for the nor a introduction page, thus Profiles()
        Profiles()
    }
}
