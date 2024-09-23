package de.connect2x.messenger.compose.view.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <T> StateFlow<T>.collectAsStateForTextField() =
    collectAsState(Dispatchers.Main.immediate)
