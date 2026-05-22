package de.connect2x.trixnity.messenger.uikit

import de.connect2x.trixnity.messenger.uikit.Utilities.checkingCombiner
import de.connect2x.trixnity.messenger.uikit.Utilities.delegate
import de.connect2x.trixnity.messenger.uikit.Utilities.delegateSuspend

abstract class Delegator<T>(protected val delegates: List<T>) {
    protected fun delegate(action: T.() -> Unit) = delegates.delegate(action)

    protected fun <R> delegateDefault(action: T.() -> WithDefault<R>): WithDefault<R> =
        delegates.delegate(::checkingCombiner, action)

    protected suspend fun delegateSuspend(action: suspend T.() -> Unit) = delegates.delegateSuspend(action)

    protected suspend fun <R> delegateDefaultSuspend(action: suspend T.() -> WithDefault<R>): WithDefault<R> =
        delegates.delegateSuspend(::checkingCombiner, action)
}
