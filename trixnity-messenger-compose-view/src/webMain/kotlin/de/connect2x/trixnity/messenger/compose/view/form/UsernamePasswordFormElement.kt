package de.connect2x.trixnity.messenger.compose.view.form

import web.dom.document
import web.html.HTMLFormElement
import web.html.HTMLInputElement
import web.html.HtmlTagName

internal fun UsernamePasswordFormElement(
    usernameInput: HTMLInputElement,
    passwordInput: HTMLInputElement,
    configure: HTMLFormElement.() -> Unit = {},
): HTMLFormElement {
    return document.createElement(HtmlTagName.form).apply {
        appendChild(usernameInput)
        appendChild(passwordInput)
        configure()
    }
}
