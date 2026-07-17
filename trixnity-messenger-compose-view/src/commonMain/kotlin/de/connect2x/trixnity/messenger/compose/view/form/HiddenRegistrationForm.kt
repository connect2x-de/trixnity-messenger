package de.connect2x.trixnity.messenger.compose.view.form

internal interface HiddenRegistrationForm {
    fun submit(username: String, password: String)
}

internal data object NoopHiddenRegistrationForm : HiddenRegistrationForm {
    override fun submit(username: String, password: String) {}
}
