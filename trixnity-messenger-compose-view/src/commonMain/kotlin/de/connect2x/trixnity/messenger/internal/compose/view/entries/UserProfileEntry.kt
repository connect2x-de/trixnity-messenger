package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.room.settings.UserProfileContainer
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.extras.UserProfileRoute
import de.connect2x.trixnity.messenger.viewmodel.room.settings.UserProfileViewModel

internal class UserProfileEntry : NavigationEntry<UserProfileRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.right() + ThreePaneScene.right()

    @Composable
    override fun Content(route: UserProfileRoute) {
        ThemedSurface(style = MaterialTheme.components.details) {
            UserProfileContainer(userProfileViewModel = rememberComponent<UserProfileViewModel>())
        }
    }
}
