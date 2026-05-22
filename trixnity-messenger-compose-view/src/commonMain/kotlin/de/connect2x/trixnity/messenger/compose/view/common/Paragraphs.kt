package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants

@Composable
fun Paragraphs(
    modifier: Modifier = Modifier,
    spaceBetween: Dp = MaterialTheme.messengerDpConstants.small,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(spaceBetween), modifier = modifier) { content() }
}
