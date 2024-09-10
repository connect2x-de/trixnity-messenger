package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppearanceSettingsColorPreview(value: Color) {
    Box(
        modifier = Modifier.clip(CircleShape)
            .background(value)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .size(24.dp)
    ) {}
}
