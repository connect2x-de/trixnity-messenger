package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
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
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.modifier.expandable
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.isMobile
import de.connect2x.trixnity.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.trixnity.messenger.compose.view.theme.messengerFocusIndicator
import kotlinx.coroutines.launch

@Composable
fun BoxScope.ThemedActionMenu(
    hoverInteractionSource: MutableInteractionSource,
    focusInteractionSource: MutableInteractionSource,
    showActionMenu: MutableState<ThemedActionMenuState>,
    actions: List<ThemedActionMenuItem>,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
    openActionMenuIcon: @Composable () -> Unit,
) {
    when {
        Platform.current.isMobile -> ThemedActionMenuMobile(showActionMenu, actions, additionalContextActions)
        else -> ThemedActionMenuDefault(
            hoverInteractionSource,
            focusInteractionSource,
            showActionMenu,
            actions,
            additionalContextActions,
            openActionMenuIcon
        )
    }
}

@Composable
private fun BoxScope.ThemedActionMenuDefault(
    hoverInteractionSource: MutableInteractionSource,
    focusInteractionSource: MutableInteractionSource,
    showActionMenu: MutableState<ThemedActionMenuState>,
    actions: List<ThemedActionMenuItem>,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
    openActionMenuIcon: @Composable () -> Unit,
) {
    val focus = focusInteractionSource.collectIsFocusedAsState()
    val hover = hoverInteractionSource.collectIsHoveredAsState()
    val isVisible =
        remember { MutableTransitionState(showActionMenu.value != ThemedActionMenuState.CLOSED || focus.value || hover.value) }
    LaunchedEffect(showActionMenu.value, focus.value, hover.value) {
        isVisible.targetState = showActionMenu.value != ThemedActionMenuState.CLOSED || focus.value || hover.value
    }

    val transition = rememberTransition(isVisible)
    val opacity = transition.animateFloat { if (it) 0.1f else 0f }

    val i18n = DI.get<I18nView>()
    val onClose = {
        showActionMenu.value = ThemedActionMenuState.CLOSED
    }
    Box(
        modifier = Modifier
            .zIndex(1f)
            .align(Alignment.TopEnd)
            .padding(
                top = 4.dp,
                end = 4.dp
            )
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = opacity.value),
            border = if (IsFocusHighlighting.current && focus.value) {
                BorderStroke(MaterialTheme.messengerFocusIndicator.borderWidth, MaterialTheme.colorScheme.onSurface)
            } else null,
            interactionSource = focusInteractionSource,
            onClick = {
                showActionMenu.value =
                    if (showActionMenu.value == ThemedActionMenuState.CLOSED) ThemedActionMenuState.ANCHORED else ThemedActionMenuState.CLOSED
            },
            modifier = Modifier
                .size(28.dp)
                .expandable(showActionMenu.value.isNotClosed())
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
                openActionMenuIcon()
            }
        }
        ThemedDropdownMenu(
            expanded = showActionMenu.value.isNotClosed(),
            onDismissRequest = { showActionMenu.value = ThemedActionMenuState.CLOSED },
            offset = DpOffset(0.dp, 0.dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                .sizeIn(maxWidth = 300.dp),
        ) {
            additionalContextActions(onClose)
            actions.forEach { action -> action.render { onClose() } }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ThemedActionMenuMobile(
    showActionMenu: MutableState<ThemedActionMenuState>,
    actions: List<ThemedActionMenuItem>,
    additionalContextActions: @Composable ColumnScope.(onClose: () -> Unit) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(false)
    val onClose = {
        coroutineScope.launch {
            bottomSheetState.hide()
        }.invokeOnCompletion {
            if (!bottomSheetState.isVisible)
                showActionMenu.value = ThemedActionMenuState.CLOSED
        }
        Unit
    }
    //Since there is no distinction between an anchored or a popup based ActionMenu on mobile, only the not closed state is relevant here
    if (showActionMenu.value.isNotClosed()) ModalBottomSheet(
        sheetState = bottomSheetState,
        onDismissRequest = { showActionMenu.value = ThemedActionMenuState.CLOSED },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 100.dp)
                .padding(bottom = 40.dp)
        ) {
            additionalContextActions(onClose)
            actions.forEach { action -> action.render { onClose() } }
        }
    }
}

open class ThemedActionMenuItem(
    open val label: String,
    open val isEnabled: Boolean = true,
    internal open val action: () -> Unit,
) {
    operator fun invoke() = action()

    @Composable
    internal fun render(onClose: () -> Unit) {
        when {
            Platform.current.isMobile -> bottomSheetItem(onClose)
            else -> dropDownMenuItem(onClose)
        }
    }

    @Composable
    internal fun dropDownMenuItem(
        onClose: () -> Unit,
    ) {
        val i18n = DI.get<I18nView>()
        Tooltip(
            enabled = !isEnabled,
            tooltip = { Text(i18n.commonButtonDisabled()) },
        ) {
            ThemedDropdownMenuItem(
                enabled = isEnabled,
                text = { Text(label) },
                onClick = {
                    onClose()
                    action()
                },
            )
        }
    }

    @Composable
    internal fun bottomSheetItem(
        onClose: () -> Unit,
    ) {
        val i18n = DI.get<I18nView>()
        Tooltip(
            enabled = !isEnabled,
            tooltip = { Text(i18n.commonButtonDisabled()) }
        ) {
            Text(
                label,
                color = if (isEnabled)
                    MaterialTheme.colorScheme.onBackground
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .clickable {
                        action()
                        onClose()
                    },
            )
        }
    }
}

enum class ThemedActionMenuState {
    ANCHORED, POPUP, CLOSED
}

fun ThemedActionMenuState.isNotClosed() = this != ThemedActionMenuState.CLOSED
