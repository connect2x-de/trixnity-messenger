package de.connect2x.trixnity.messenger.compose.view.common.icons

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun EditIcon(icon: ImageVector, description: String) {
    Icon(icon, description, Modifier.padding(10.dp))
}
