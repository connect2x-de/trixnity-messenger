package de.connect2x.trixnity.messenger.util

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val log = KotlinLogging.logger {  }

interface BackHandler {
    fun goBack()

    fun registerBackCallback(callback: BackCallback)

    fun Lifecycle.registerBackCallbackWithLifecycle(callback: BackCallback)

    fun unregisterCallback(callback: BackCallback)

    val stack: StateFlow<ArrayDeque<BackCallback>>
    companion object {
        val PRIORITY_DEFAULT = 0
        val PRIORITY_WIZARD = 1
        val PRIORITY_SELF_VERIFICATION = 2
    }
}

data class BackCallback(val priority: Int = BackHandler.PRIORITY_DEFAULT, val onBack: () -> Unit)

class BackHandlerImpl: BackHandler {
    private val _backCallbackStack: MutableStateFlow<ArrayDeque<BackCallback>> = MutableStateFlow(ArrayDeque())
    override val stack: StateFlow<ArrayDeque<BackCallback>> = _backCallbackStack.asStateFlow()

    /**
     * Registers a callback to the backCallBackStack
     *
     * @param callback The callback to be added with higher priority values taking precedence over lower ones in the stack evaluation.
     * @param lifecycle An optional lifecycle of the callback used to remove the callback from the stack once it is destroyed.
     */
    override fun registerBackCallback(callback: BackCallback) {
        val indexToAdd = _backCallbackStack.value.indexOfFirst { it.priority < callback.priority }.coerceAtLeast(0)
        _backCallbackStack.value.add(indexToAdd, callback)
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
        if (!_backCallbackStack.value.remove(callback)) {
            log.warn { "Can't remove callback since its not on the stack" }
        }
    }

    override fun goBack() {
        _backCallbackStack.value.firstOrNull()?.onBack() ?: log.warn { "No elements on the stack" }
    }


}
