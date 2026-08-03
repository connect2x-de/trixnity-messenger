package de.connect2x.trixnity.messenger.internal.uia

import de.connect2x.trixnity.clientserverapi.client.UIA

internal interface UIAStateHolder {
    val step: UIA.Step<*>

    fun provideAction(action: suspend () -> Result<UIA<*>>)

    fun provideStep(step: UIA.Step<*>)

    suspend fun executeAction(): Result<UIA<*>>

    fun clear()
}

internal fun UIAStateHolder(): UIAStateHolder {
    return UIAStateHolderImpl()
}

private class UIAStateHolderImpl : UIAStateHolder {

    private var action: (suspend () -> Result<UIA<*>>)? = null
    private var currentStep: UIA.Step<*>? = null

    override val step: UIA.Step<*>
        get() = checkNotNull(currentStep)

    override fun provideAction(action: suspend () -> Result<UIA<*>>) {
        this.action = action
    }

    override fun provideStep(step: UIA.Step<*>) {
        this.currentStep = step
    }

    override suspend fun executeAction(): Result<UIA<*>> {
        return checkNotNull(action).invoke()
    }

    override fun clear() {
        this.action = null
        this.currentStep = null
    }
}
