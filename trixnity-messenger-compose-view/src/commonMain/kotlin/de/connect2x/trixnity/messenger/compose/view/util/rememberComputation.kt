package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun <T> rememberComputation(key1: Any, callback: () -> T): T? {
    val state = remember { mutableStateOf<T?>(null) }
    LaunchedEffect(key1) { launch(Dispatchers.Default) { state.value = callback() } }
    return state.value
}

@Composable
internal fun <T> rememberComputation(key1: Any, key2: Any, callback: () -> T): T? {
    val state = remember { mutableStateOf<T?>(null) }
    LaunchedEffect(key1, key2) { launch(Dispatchers.Default) { state.value = callback() } }
    return state.value
}

@Composable
internal fun <T> rememberComputation(key1: Any, key2: Any, key3: Any, callback: () -> T): T? {
    val state = remember { mutableStateOf<T?>(null) }
    LaunchedEffect(key1, key2, key3) { launch(Dispatchers.Default) { state.value = callback() } }
    return state.value
}

@Composable
internal fun <T> rememberComputation(vararg keys: Any?, callback: () -> T): T? {
    val state = remember { mutableStateOf<T?>(null) }
    LaunchedEffect(*keys) { launch(Dispatchers.Default) { state.value = callback() } }
    return state.value
}
