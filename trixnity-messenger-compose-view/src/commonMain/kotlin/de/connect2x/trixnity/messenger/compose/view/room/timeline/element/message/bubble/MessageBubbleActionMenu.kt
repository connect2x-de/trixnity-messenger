package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.contextMenuActions
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedActionMenu
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedActionMenuState
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel


@Composable
fun BoxScope.MessageBubbleActionMenu(
    holder: BaseTimelineElementHolderViewModel,
    showActionMenu: MutableState<ThemedActionMenuState>,
    onOpenMetadata: () -> Unit,
    onReactToMessage: () -> Unit,
    hoverInteractionSource: MutableInteractionSource,
    focusInteractionSource: MutableInteractionSource,
    onRedact: () -> Unit,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
) {
    val i18n = DI.get<I18nView>()
    ThemedActionMenu(
        hoverInteractionSource,
        focusInteractionSource,
        showActionMenu,
        holder.contextMenuActions(i18n, onOpenMetadata, onReactToMessage, onRedact),
        additionalContextActions,
        {
            Icon(Icons.Default.ExpandMore, null, tint = Color.White)
        },
        Modifier.align(Alignment.TopEnd).padding(4.dp)
    )
}
