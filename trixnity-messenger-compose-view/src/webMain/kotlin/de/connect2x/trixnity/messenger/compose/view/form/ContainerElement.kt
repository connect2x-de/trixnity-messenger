package de.connect2x.trixnity.messenger.compose.view.form

import web.dom.document
import web.html.HTMLDivElement
import web.html.HTMLFormElement
import web.html.HTMLStyleElement
import web.html.HtmlTagName

internal fun ContainerElement(style: HTMLStyleElement, form: HTMLFormElement): HTMLDivElement {
    return document.createElement(HtmlTagName.div).apply {
        appendChild(style)
        appendChild(form)
    }
}
