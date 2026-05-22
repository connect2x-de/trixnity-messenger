package de.connect2x.trixnity.messenger.integrationtests.util

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import io.ktor.util.reflect.*
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.first

private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.util.ViewModelsKt")

@Suppress("UNCHECKED_CAST")
suspend fun <C : Any, W : Any, T : Any> Value<ChildStack<C, W>>.waitFor(clazz: KClass<T>): T {
    return this.toFlow()
        .first { childStack ->
            log.debug { "wait for $clazz, active: ${childStack.active.instance}" }
            childStack.active.instance.instanceOf(clazz)
        }
        .active
        .instance as T
}
