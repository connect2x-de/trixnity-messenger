package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.room.timeline.element.message.baseMenuActions
import de.connect2x.messenger.compose.view.room.timeline.element.message.render
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import kotlinx.coroutines.launch


@Composable
fun BoxScope.MessageBubbleActionMenu(
    holder: BaseTimelineElementHolderViewModel,
    hoverMessage: State<Boolean>,
    showActionMenu: MutableState<Boolean>,
    onMessageInfo: () -> Unit,
    onReactToMessage: () -> Unit,
    onInfoClassic: () -> Unit, // TODO remove
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
) {
    val i18n = DI.current.get<I18nView>()
    when {
        Platform.current.isMobile -> MessageBubbleActionMenuMobile(
            showActionMenu,
            additionalContextActions,
            holder,
            i18n,
            onMessageInfo,
            onInfoClassic,
            onReactToMessage,
        )

        else -> MessageBubbleActionMenuDefault(
            showActionMenu,
            holder,
            hoverMessage,
            i18n,
            additionalContextActions,
            onMessageInfo,
            onInfoClassic,
            onReactToMessage,
        )
    }
}

@Composable
private fun BoxScope.MessageBubbleActionMenuDefault(
    showActionMenu: MutableState<Boolean>,
    holder: BaseTimelineElementHolderViewModel,
    hoverMessage: State<Boolean>,
    i18n: I18nView,
    additionalContextActions: @Composable() (ColumnScope.(onClose: () -> Unit) -> Unit),
    onMessageInfo: () -> Unit,
    onInfoClassic: () -> Unit,
    onReactToMessage: () -> Unit,
) {
    val onClose = {
        showActionMenu.value = false
    }
    Box(
        Modifier.Companion
            .align(Alignment.TopEnd)
            .padding(
                top = 4.dp,
                end = if (holder.isByMe) 14.dp else 4.dp
            )
            .defaultMinSize(minHeight = 24.dp, minWidth = 24.dp) // 24dp Material Icon
    ) {
        AnimatedVisibility(
            hoverMessage.value,
            Modifier
                .clip(CircleShape)
        ) {
            Box(
                Modifier
                    .background(Color.Black.copy(alpha = 0.1f))
                    .clickable { showActionMenu.value = showActionMenu.value.not() }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .indication(
                        indication = null,
                        interactionSource = MutableInteractionSource()
                    )
            ) {
                Icon(Icons.Default.ExpandMore, i18n.commonContextMenu(), tint = Color.White)
            }
        }
        DropdownMenu(
            expanded = showActionMenu.value,
            onDismissRequest = { showActionMenu.value = false },
            offset = DpOffset(0.dp, 0.dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                .sizeIn(maxWidth = 300.dp),
        ) {
            additionalContextActions(onClose)
            holder.baseMenuActions(i18n, onMessageInfo, onInfoClassic, onReactToMessage).forEach { action ->
                action.render { onClose() }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BoxScope.MessageBubbleActionMenuMobile(
    showActionMenu: MutableState<Boolean>,
    additionalContextActions: @Composable() (ColumnScope.(onClose: () -> Unit) -> Unit),
    holder: BaseTimelineElementHolderViewModel,
    i18n: I18nView,
    onMessageInfo: () -> Unit,
    onInfoClassic: () -> Unit,
    onReactToMessage: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(false)
    val onClose = {
        coroutineScope.launch {
            bottomSheetState.hide()
        }.invokeOnCompletion {
            if (!bottomSheetState.isVisible)
                showActionMenu.value = false
        }
        Unit
    }
    if (showActionMenu.value) {
        ModalBottomSheet(
            sheetState = bottomSheetState,
            onDismissRequest = { showActionMenu.value = false },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 100.dp)
                    .padding(bottom = 40.dp)
            ) {
                additionalContextActions(onClose)
                holder.baseMenuActions(i18n, onMessageInfo, onInfoClassic, onReactToMessage).forEach { action ->
                    action.render {
                        onClose()
                    }
                }
            }
        }
    }
}
