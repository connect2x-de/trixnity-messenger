package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun StateFlow<Boolean?>.collectAsStateForLoadingIndicator(
    timeout: Duration = 120.milliseconds
): State<Boolean> =
    produceState(false, this) {
        collectLatest {
            delay(timeout)
            value = it != false
        }
    }
