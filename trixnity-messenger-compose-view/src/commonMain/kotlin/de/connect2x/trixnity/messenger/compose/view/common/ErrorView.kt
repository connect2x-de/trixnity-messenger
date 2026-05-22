package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorView(errorMessage: String) {
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.error)) {
        Text(errorMessage, Modifier.fillMaxWidth().padding(20.dp), color = MaterialTheme.colorScheme.onError)
    }
}
