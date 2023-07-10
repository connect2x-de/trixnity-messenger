package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext

interface VerificationStepCompareViewModelFactory {
    fun newVerificationStepCompareViewModel(
        viewModelContext: ViewModelContext,
        decimals: List<Int>,
        emojisWithoutTranslation: List<Pair<Int, String>>,
        onAccept: () -> Unit,
        onDecline: () -> Unit,
    ): VerificationStepCompareViewModel {
        return VerificationStepCompareViewModelImpl(
            viewModelContext, decimals, emojisWithoutTranslation, onAccept, onDecline
        )
    }
}

interface VerificationStepCompareViewModel {
    val decimals: List<Int>
    val emojis: List<Pair<String, Map<String, String>>>

    fun accept()
    fun decline()
}

open class VerificationStepCompareViewModelImpl(
    viewModelContext: ViewModelContext,
    override val decimals: List<Int>,
    emojisWithoutTranslation: List<Pair<Int, String>>,
    private val onAccept: () -> Unit,
    private val onDecline: () -> Unit,
) : ViewModelContext by viewModelContext, VerificationStepCompareViewModel {

    override val emojis = emojisWithoutTranslation.map { (number, _) ->
        emojisWithTranslation[number] ?: throw IllegalArgumentException("Cannot find emoji for number $number.")
    }

    override fun accept() {
        onAccept()
    }

    override fun decline() {
        onDecline()
    }

}
