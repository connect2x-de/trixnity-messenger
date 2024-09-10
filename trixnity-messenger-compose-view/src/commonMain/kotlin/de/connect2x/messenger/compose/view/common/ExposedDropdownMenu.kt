package de.connect2x.messenger.compose.view.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlin.time.Duration.Companion.milliseconds

/**
 * A basic implementation of the Exposed Dropdown Menu component
 *
 * [Material](https://material.io/components/menus#exposed-dropdown-menu)
 *
 * [Original](https://www.jetpackcompose.app/snippets/ExposedDropdownMenu)
 */
@Composable
fun ExposedDropdownMenu(
    items: List<String>,
    selected: String? = items[0],
    canBeCleared: Boolean = false,
    label: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    onItemSelected: (String?) -> Unit,
) {
    val i18n = DI.current.get<I18nView>()
    val platform = Platform.current
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filter {
                if (platform.isMobile) it is PressInteraction.Press || it is FocusInteraction.Focus
                else it is FocusInteraction.Focus
            }
            .collect {
                isFocused = true
                expanded = !expanded
                delay(100.milliseconds)
                isFocused = false
            }
    }
    val clickModifier = Modifier.clickable { if (isFocused.not()) expanded = !expanded }
    ExposedDropdownMenuStack(
        textField = {
            OutlinedTextField(
                value = selected ?: "",
                onValueChange = {},
                label = label,
                interactionSource = interactionSource,
                readOnly = true,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
                        if (canBeCleared) {
                            Icon(
                                Icons.Default.Clear,
                                i18n.commonRemove(),
                                Modifier.buttonPointerModifier().clickable { onItemSelected(null) },
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        val rotation by animateFloatAsState(if (expanded) 180F else 0F)
                        Icon(
                            Icons.Default.ArrowDropDown,
                            i18n.commonSelect(),
                            Modifier.rotate(rotation).buttonPointerModifier()
                                .then(if (platform.isMobile) Modifier else clickModifier),
                        )
                    }
                },
                modifier = modifier,
            )
        },
        dropdownMenu = { boxWidth, itemHeight ->
            Box(
                Modifier
                    .width(boxWidth)
                    .wrapContentSize(Alignment.TopStart)
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            modifier = Modifier
                                .height(itemHeight)
                                .width(boxWidth)
                                .buttonPointerModifier(),
                            onClick = {
                                expanded = false
                                onItemSelected(item)
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ExposedDropdownMenuStack(
    textField: @Composable () -> Unit,
    dropdownMenu: @Composable (boxWidth: Dp, itemHeight: Dp) -> Unit
) {
    SubcomposeLayout { constraints ->
        val textFieldPlaceable =
            subcompose(ExposedDropdownMenuSlot.TextField, textField).first().measure(constraints)
        val dropdownPlaceable = subcompose(ExposedDropdownMenuSlot.Dropdown) {
            dropdownMenu(textFieldPlaceable.width.toDp(), textFieldPlaceable.height.toDp())
        }.first().measure(constraints)
        layout(textFieldPlaceable.width, textFieldPlaceable.height) {
            textFieldPlaceable.placeRelative(0, 0)
            dropdownPlaceable.placeRelative(0, textFieldPlaceable.height)
        }
    }
}

private enum class ExposedDropdownMenuSlot { TextField, Dropdown }