package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp).then(modifier)) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}