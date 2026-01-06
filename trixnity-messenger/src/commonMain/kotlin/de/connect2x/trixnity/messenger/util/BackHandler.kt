package de.connect2x.trixnity.messenger.util

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val log = KotlinLogging.logger { }

interface BackHandler {
    /**
     * Executes the newest enabled callback with the highest priority
     */
    fun goBack()

    /**
     * Registers a callback to the backCallbackStack
     *
     * @param callback The callback to be added with higher priority values taking precedence over lower ones in the stack evaluation.
     */
    fun registerBackCallback(callback: BackCallback)

    /**
     * Registers a callback to the BackCallbackStack and unregisters it on destruction of the lifecycle
     *
     * @param callback The callback to be added with higher priority values taking precedence over lower ones in the stack evaluation.
     */
    fun Lifecycle.registerBackCallbackWithLifecycle(callback: BackCallback)

    /**
     * Unregisters a callback from the BackCallbackStack
     */
    fun unregisterCallback(callback: BackCallback)

    /**
     * The stack of registered BackCallbacks ordered by priority and time of creation
     */
    val stack: List<BackCallback>

    companion object {
        const val PRIORITY_DEFAULT = 0
        const val PRIORITY_WIZARD = 1
        const val PRIORITY_VERIFICATION = 2
    }
}

data class BackCallback(
    val priority: Int = BackHandler.PRIORITY_DEFAULT,
    val enabled: StateFlow<Boolean> = MutableStateFlow(true),
    val onBack: () -> Unit
)

class BackHandlerImpl : BackHandler {
    private val _backCallbackStack: MutableList<BackCallback> = mutableListOf()
    override val stack: List<BackCallback> = _backCallbackStack

    override fun registerBackCallback(callback: BackCallback) {
        val indexToAdd = _backCallbackStack.indexOfFirst { listElement -> callback.priority >= listElement.priority }
            .coerceAtLeast(0)
        _backCallbackStack.add(indexToAdd, callback)
    }

    override fun Lifecycle.registerBackCallbackWithLifecycle(callback: BackCallback) {
        registerBackCallback(callback)

        this.let {
            it.doOnDestroy {
                unregisterCallback(callback)
            }
        }
    }

    override fun unregisterCallback(callback: BackCallback) {
        if (!_backCallbackStack.remove(callback)) {
            log.warn { "Can't remove callback since its not on the stack" }
        }
    }

    override fun goBack() {
        _backCallbackStack.firstOrNull { it.enabled.value }?.onBack() ?: log.warn { "No elements on the stack" }
    }
}
