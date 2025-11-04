package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.theme.components

@OptIn(ExperimentalMaterial3Api::class)
data class SelectStyle(
    val anchor: TextFieldColors,
    val menu: SurfaceStyle,
    val item: DropdownMenuItemStyle,
) {
    companion object {
        @Composable
        fun default(
            anchor: TextFieldColors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            menu: SurfaceStyle = SurfaceStyle.default(
                color = MenuDefaults.containerColor,
                tonalElevation = MenuDefaults.TonalElevation,
                shadowElevation = MenuDefaults.ShadowElevation,
                shape = MenuDefaults.shape,
            ),
            itemStyle: DropdownMenuItemStyle = DropdownMenuItemStyle.default(
                contentPadding = PaddingValues(horizontal = 10.dp),
            ),
        ) = SelectStyle(
            anchor,
            menu,
            itemStyle,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ThemedSelect(
    value: T,
    onValueChange: (T) -> Unit,
    options: List<T>,
    modifier: Modifier = Modifier,
    style: SelectStyle = MaterialTheme.components.select,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    leadingIcon: @Composable ((T) -> Unit)? = null,
    render: (T) -> String,
) {
    val expanded = remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded.value,
        onExpandedChange = { expanded.value = it },
    ) {
        val textColor = LocalTextStyle.current.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            when {
                !true -> style.anchor.disabledTextColor
                focused -> style.anchor.focusedTextColor
                else -> style.anchor.unfocusedTextColor
            }
        }
        val mergedTextStyle = LocalTextStyle.current.merge(TextStyle(color = textColor))
        val density = LocalDensity.current

        val labelModifier = label?.let {
            Modifier
                // Merge semantics at the beginning of the modifier chain to ensure
                // padding is considered part of the text field.
                .semantics(mergeDescendants = true) {}
                .padding(top = with(density) { 8.sp.toDp() })
        } ?: Modifier

        Box(
            modifier = Modifier
                .buttonPointerModifier(enabled)
                .toggleable(expanded.value, interactionSource, null, enabled, Role.DropdownList) {
                    expanded.value = it
                }
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .then(labelModifier).defaultMinSize(
                    minWidth = OutlinedTextFieldDefaults.MinWidth,
                    minHeight = OutlinedTextFieldDefaults.MinHeight
                ),
            propagateMinConstraints = true,
        ) {
            OutlinedTextFieldDefaults.DecorationBox(
                value = render(value),
                visualTransformation = VisualTransformation.None,
                innerTextField = {
                    Text(
                        render(value),
                        style = mergedTextStyle,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        maxLines = 1,
                        minLines = 1,
                    )
                },
                placeholder = null,
                label = label,
                leadingIcon = leadingIcon?.let { @Composable { leadingIcon(value) } },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded.value)
                },
                prefix = null,
                suffix = null,
                supportingText = null,
                singleLine = true,
                enabled = true,
                isError = false,
                interactionSource = interactionSource,
                colors = style.anchor,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = style.anchor,
                        shape = OutlinedTextFieldDefaults.shape,
                        modifier = Modifier.indication(interactionSource, LocalIndication.current),
                    )
                }
            )
        }

        ThemedExposedDropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            matchAnchorWidth = true,
            style = style.menu,
        ) {
            for (option in options) {
                key(option) {
                    val focusRequester = remember { FocusRequester() }

                    ThemedDropdownMenuItem(
                        leadingIcon = leadingIcon?.let { @Composable { leadingIcon(option) } },
                        text = { Text(render(option)) },
                        onClick = {
                            expanded.value = false
                            onValueChange(option)
                        },
                        modifier = Modifier.focusRequester(focusRequester),
                        style = style.item,
                    )

                    LaunchedEffect(Unit) {
                        if (value == option) {
                            focusRequester.requestFocus()
                        }
                    }
                }
            }
        }
    }
}
