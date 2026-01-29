package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.koin.core.Koin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.parameter.emptyParametersHolder
import org.koin.core.qualifier.Qualifier

// inspired by https://github.com/InsertKoinIO/koin/blob/main/projects/compose/koin-compose/src/commonMain/kotlin/org/koin/compose/Inject.kt#L36
@Composable
inline fun <reified T : Any> ProvidableCompositionLocal<Koin>.get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T {
    val di = current
    val currentParameters by rememberUpdatedState(parameters)
    return remember(qualifier) {
        di.get(qualifier) {
            currentParameters?.invoke() ?: emptyParametersHolder()
        }
    }
}

@Composable
inline fun <reified T : Any> ProvidableCompositionLocal<Koin>.getOrNull(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T? {
    val di = current
    val currentParameters by rememberUpdatedState(parameters)
    return remember(qualifier) {
        di.getOrNull(qualifier) {
            currentParameters?.invoke() ?: emptyParametersHolder()
        }
    }
}
