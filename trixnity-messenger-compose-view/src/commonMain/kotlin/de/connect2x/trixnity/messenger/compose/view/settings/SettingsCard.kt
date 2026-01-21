package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun DevInfoCard(title: String,
                icon: ImageVector? = null,
                content: @Composable ColumnScope.() -> Unit
) {
    SettingsCard(
        title,
        icon,
        CardColors(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface),{
            content()
        })
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector? = null,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(Modifier.padding(bottom = 10.dp), colors = colors) {
        Column(Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) Icon(imageVector = icon, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            HorizontalDivider(Modifier.fillMaxWidth().padding(vertical = 10.dp))
            content()
        }
    }
}
