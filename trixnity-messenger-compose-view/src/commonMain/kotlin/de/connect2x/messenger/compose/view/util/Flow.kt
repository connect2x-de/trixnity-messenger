package de.connect2x.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun Flow<Boolean?>.collectAsStateForLoadingIndicator(
    initial: Boolean = false,
    timeout: Duration = 120.milliseconds
): State<Boolean> {
    var showIndicator = remember {
        mutableStateOf(false)
    }
    val isLoading by this.collectAsState(initial)

    LaunchedEffect(isLoading) {
        if (isLoading != false) {
            delay(timeout)
            showIndicator.value = true
        } else {
            showIndicator.value = false
        }
    }

    return showIndicator
}
