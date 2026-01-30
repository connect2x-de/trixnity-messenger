package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants

data class RadioButtonOption(
    val text: String,
    val isSelected: () -> Boolean,
    val onSelect: () -> Unit,
)

@Composable
fun ColumnScope.RadioButtonSelection(
    vararg radioButtonOptions: RadioButtonOption,
) {
    radioButtonOptions.forEach { option ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                option.onSelect()
            }
                .buttonPointerModifier(),
        ) {
            RadioButton(
                selected = option.isSelected(),
                onClick = {
                    option.onSelect()
                },
            )
            Text(
                text = option.text,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = MaterialTheme.messengerDpConstants.small),
            )
        }
    }
}
