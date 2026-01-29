package de.connect2x.trixnity.messenger.compose.view.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.TWO_PANE_THRESHOLD
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.room.settings.ExtrasPaneContentSwitch
import de.connect2x.trixnity.messenger.compose.view.room.timeline.RoomContentSwitch
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.viewmodel.room.RoomViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import kotlinx.coroutines.flow.map


const val TIMELINE_WEIGHT = 0.6f
const val SETTINGS_WEIGHT = 1f - TIMELINE_WEIGHT

interface RoomView {
    @Composable
    fun create(roomViewModel: RoomViewModel)
}

@Composable
fun Room(roomViewModel: RoomViewModel) {
    DI.get<RoomView>().create(roomViewModel)
}

class RoomViewImpl : RoomView {
    @Composable
    override fun create(roomViewModel: RoomViewModel) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isSinglePane = this@BoxWithConstraints.maxWidth < TWO_PANE_THRESHOLD.dp

            val isSettingsShown = remember {
                roomViewModel.extrasStack.toFlow().map { it.active.configuration is ExtrasRouter.Config.RoomSettings }
            }.collectAsState(initial = false).value
            val isExtrasShown = remember {
                roomViewModel.extrasStack.toFlow().map { it.active.configuration !is ExtrasRouter.Config.None }
            }.collectAsState(initial = false).value

            Row(modifier = Modifier.fillMaxSize()) {

                // Timeline Column
                if (!isExtrasShown || !isSinglePane) Box(
                    modifier = Modifier
                        .weight(if (isSinglePane) 1F else TIMELINE_WEIGHT)
                ) {
                    RoomContentSwitch(roomViewModel.timelineStack, !isSettingsShown, !isExtrasShown)
                }

                // Pane Divider
                if (isExtrasShown && !isSinglePane) VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )

                // Extras Pane
                if (isExtrasShown) {
                    ThemedSurface(
                        style = MaterialTheme.components.details,
                        modifier = Modifier.weight(if (isSinglePane) 1F else SETTINGS_WEIGHT)
                    ) {
                        ExtrasPaneContentSwitch(roomViewModel.extrasStack, isSinglePane)
                    }
                }
            }
        }
    }
}
