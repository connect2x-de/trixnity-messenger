package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.room.timeline.element.message.contextMenuActions
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.components.ThemedDropdownMenu
import de.connect2x.messenger.compose.view.theme.messengerFocusIndicator
import de.connect2x.messenger.compose.view.util.LocalRovingFocus
import de.connect2x.messenger.compose.view.util.LocalRovingFocusItem
import de.connect2x.messenger.compose.view.util.rovingFocusChild
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import kotlinx.coroutines.launch


@Composable
fun BoxScope.MessageBubbleActionMenu(
    holder: BaseTimelineElementHolderViewModel,
    showActionMenu: MutableState<Boolean>,
    onOpenMetadata: () -> Unit,
    onReactToMessage: () -> Unit,
    interactionSource: MutableInteractionSource,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
) {
    when {
        Platform.current.isMobile -> MessageBubbleActionMenuMobile(
            holder,
            showActionMenu,
            onOpenMetadata,
            onReactToMessage,
            additionalContextActions,
        )

        else -> MessageBubbleActionMenuDefault(
            holder,
            interactionSource,
            showActionMenu,
            onOpenMetadata,
            onReactToMessage,
            additionalContextActions,
        )
    }
}

@Composable
private fun BoxScope.MessageBubbleActionMenuDefault(
    holder: BaseTimelineElementHolderViewModel,
    interactionSource: MutableInteractionSource,
    showActionMenu: MutableState<Boolean>,
    onOpenMetadata: () -> Unit,
    onReactToMessage: () -> Unit,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
) {
    val focus = interactionSource.collectIsFocusedAsState()
    val hover = interactionSource.collectIsHoveredAsState()
    val isVisible = remember { MutableTransitionState(showActionMenu.value || focus.value || hover.value) }
    LaunchedEffect(showActionMenu.value, focus.value, hover.value) {
        isVisible.targetState = showActionMenu.value || focus.value || hover.value
    }

    val transition = rememberTransition(isVisible)
    val opacity = transition.animateFloat { if (it) 0.1f else 0f }

    val focusContainer = LocalRovingFocus.current
    val focusItem = LocalRovingFocusItem.current

    val i18n = DI.get<I18nView>()
    val onClose = {
        showActionMenu.value = false
    }
    Box(
        modifier = Modifier
            .zIndex(1f)
            .align(Alignment.TopEnd)
            .padding(
                top = 4.dp,
                end = if (holder.isByMe) 14.dp else 4.dp
            )
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = opacity.value),
            border = if (IsFocusHighlighting.current && focus.value) {
                BorderStroke(MaterialTheme.messengerFocusIndicator.borderWidth, MaterialTheme.colorScheme.onSurface)
            } else null,
            onClick = {
                showActionMenu.value = showActionMenu.value.not()
                focusContainer?.selectItem(focusItem?.key, shouldFocus = true)
            },
            modifier = Modifier
                .size(28.dp)
                .rovingFocusChild()
                .semantics {
                    role = Role.Button
                    contentDescription = i18n.commonContextMenu()
                }
        ) {
            transition.AnimatedVisibility(
                modifier = Modifier.buttonPointerModifier(enabled = true),
                visible = { it },
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Icon(Icons.Default.ExpandMore, null, tint = Color.White)
            }
        }
        ThemedDropdownMenu(
            expanded = showActionMenu.value,
            onDismissRequest = { showActionMenu.value = false },
            offset = DpOffset(0.dp, 0.dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                .sizeIn(maxWidth = 300.dp),
        ) {
            additionalContextActions(onClose)
            holder.contextMenuActions(i18n, onOpenMetadata, onReactToMessage)
                .forEach { action -> action.render { onClose() } }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MessageBubbleActionMenuMobile(
    holder: BaseTimelineElementHolderViewModel,
    showActionMenu: MutableState<Boolean>,
    onOpenMetadata: () -> Unit,
    onReactToMessage: () -> Unit,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
) {
    val i18n = DI.get<I18nView>()
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
    if (showActionMenu.value) ModalBottomSheet(
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
            holder.contextMenuActions(i18n, onOpenMetadata, onReactToMessage)
                .forEach { action -> action.render { onClose() } }
        }
    }
}
