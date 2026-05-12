package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun RunningText(text: String, color: Color = Color.Unspecified, style: TextStyle = LocalTextStyle.current) {
    Text(text, modifier = Modifier.padding(bottom = 5.dp), color = color, style = style)
}
