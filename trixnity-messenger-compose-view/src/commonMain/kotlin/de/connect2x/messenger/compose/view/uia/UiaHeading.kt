package de.connect2x.messenger.compose.view.uia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun UiaHeading(text: String) =
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.align(Alignment.Center)) {
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    }
