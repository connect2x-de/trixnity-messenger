package de.connect2x.messenger.compose.view.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TextLabel(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.25f), shape = RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = color, shape = RoundedCornerShape(16.dp))
            .padding(start = 5.dp, end = 5.dp, top = 2.dp, bottom = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
