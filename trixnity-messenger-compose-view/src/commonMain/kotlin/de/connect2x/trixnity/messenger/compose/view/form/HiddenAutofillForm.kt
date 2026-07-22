package de.connect2x.trixnity.messenger.compose.view.form

internal interface HiddenAutofillForm {
    fun layoutUsername(left: Int, top: Int, width: Int, height: Int)

    fun layoutPassword(left: Int, top: Int, width: Int, height: Int)
}

internal data object NoopHiddenAutofillForm : HiddenAutofillForm {
    override fun layoutPassword(left: Int, top: Int, width: Int, height: Int) {}

    override fun layoutUsername(left: Int, top: Int, width: Int, height: Int) {}
}
