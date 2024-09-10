package de.connect2x.messenger.desktop.utils

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal val log = KotlinLogging.logger { }

internal suspend inline fun <reified T : Any> Value<ChildStack<*, *>>.waitFor(): T {
    return this.toFlow()
        .map { it.active.instance }
        .onEach {
            log.info { "wait for ${T::class}, active: ${it::class}" }
        }
        .filterIsInstance<T>()
        .first()
}
