package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.room.settings.addmembers.AddMembersContainer
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.extras.AddMembersRoute
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModel

internal class AddMembersEntry : NavigationEntry<AddMembersRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.right() + ThreePaneScene.right()

    @Composable
    override fun Content(route: AddMembersRoute) {
        ThemedSurface(style = MaterialTheme.components.details) {
            AddMembersContainer(addMembersViewModel = rememberComponent<AddMembersViewModel>())
        }
    }
}
