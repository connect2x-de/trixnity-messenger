package de.connect2x.trixnity.messenger.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.KoinScopeComponent
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

inline fun <reified T : Any> KoinComponent.getOrNull(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T? {
    return if (this is KoinScopeComponent) {
        scope.getOrNull(qualifier, parameters)
    } else {
        getKoin().getOrNull(qualifier, parameters)
    }
}
