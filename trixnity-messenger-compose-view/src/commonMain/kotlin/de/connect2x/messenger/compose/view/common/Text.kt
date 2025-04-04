package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RunningText(text: String, color: Color = Color.Unspecified, style: TextStyle = LocalTextStyle.current) {
    Text(text, modifier = Modifier.padding(bottom = 5.dp), color = color, style = style)
}

@Composable
fun SelectableText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    fontWeight: FontWeight? = null,
    ) {
    SelectionContainer(modifier) {
        Text(text, style = style, maxLines = maxLines, overflow = overflow, softWrap = softWrap, fontWeight = fontWeight)
    }
}
