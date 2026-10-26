package de.connect2x.trixnity.messenger.compose.view.space

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedHorizontalDivider
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.util.inputFocusNavigation

@Composable
internal fun SpaceSearchButton(showSearch: Boolean, onChange: (Boolean) -> Unit) {
    val i18n = DI.get<I18nView>()
    val label = if (showSearch) i18n.accountDeactivateSearch() else i18n.accountActivateSearch()
    Tooltip({ Text(label) }) {
        ThemedIconButton(style = MaterialTheme.components.commonIconButton, onClick = { onChange(!showSearch) }) {
            Icon(if (showSearch) Icons.Default.SearchOff else Icons.Default.Search, label)
        }
    }
}

@Composable
internal fun SpaceSearchBanner(
    showSearch: Boolean,
    searchTerm: TextFieldValue,
    onSearchChange: (TextFieldValue) -> Unit,
    onClose: () -> Unit,
    label: String,
) {
    val focusRequester = remember { FocusRequester() }
    AnimatedVisibility(showSearch, enter = expandVertically(), exit = shrinkVertically()) {
        ThemedSurface(style = MaterialTheme.components.commonBanner, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchTerm,
                    onValueChange = onSearchChange,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(10.dp)
                            .heightIn(40.dp)
                            .focusRequester(focusRequester)
                            .inputFocusNavigation()
                            .onKeyEvent {
                                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                                    onClose()
                                    true
                                } else {
                                    false
                                }
                            },
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, label) },
                    label = { Text(label) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
                )
                ThemedHorizontalDivider()
            }
        }
    }
    LaunchedEffect(showSearch) {
        if (showSearch) {
            focusRequester.requestFocus()
        }
    }
}
