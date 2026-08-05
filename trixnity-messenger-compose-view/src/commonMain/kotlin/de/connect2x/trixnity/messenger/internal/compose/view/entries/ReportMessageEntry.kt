package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.room.timeline.MessageReport
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay.OverlayScene
import de.connect2x.trixnity.messenger.internal.routes.ReportMessageRoute
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportMessageViewModel

internal class ReportMessageEntry : NavigationEntry<ReportMessageRoute> {

    override val metadata: Map<String, Any> = OverlayScene.overlay()

    @Composable
    override fun Content(route: ReportMessageRoute) {
        MessageReport(reportToMessageViewModel = rememberComponent<ReportMessageViewModel>())
    }
}
