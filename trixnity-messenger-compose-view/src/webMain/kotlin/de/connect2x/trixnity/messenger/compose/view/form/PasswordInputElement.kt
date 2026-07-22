package de.connect2x.trixnity.messenger.compose.view.form

import web.autofill.AutoFill
import web.dom.document
import web.html.HTMLInputElement
import web.html.HtmlTagName
import web.html.InputType
import web.html.password

internal fun PasswordInputElement(
    name: String,
    autocomplete: AutoFill,
    configure: HTMLInputElement.() -> Unit = {},
): HTMLInputElement {
    return document.createElement(HtmlTagName.input).apply {
        this.type = InputType.password
        this.name = name
        this.autocomplete = autocomplete
        this.spellcheck = false
        this.tabIndex = -1

        configure()
    }
}
