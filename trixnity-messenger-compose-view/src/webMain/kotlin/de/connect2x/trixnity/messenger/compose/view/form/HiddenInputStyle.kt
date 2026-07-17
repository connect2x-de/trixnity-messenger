package de.connect2x.trixnity.messenger.compose.view.form

import web.cssom.CSS
import web.dom.document
import web.html.HTMLStyleElement
import web.html.HtmlTagName

internal const val HIDDEN_INPUT_ID_ATTR = "data-cmp-hidden-input-id"

internal fun HiddenInputHTMLStyleElement(hiddenInputId: String): HTMLStyleElement {
    val escapedId = CSS.escape(hiddenInputId)

    return document.createElement(HtmlTagName.style).apply {
        textContent =
            """
            input[$HIDDEN_INPUT_ID_ATTR="$escapedId"]::selection {
              background-color: transparent;
              color: transparent;
            }
            
            input[$HIDDEN_INPUT_ID_ATTR="$escapedId"] {
              background-color: transparent;
              position: fixed;
              z-index: 9999;
              border: 0;
              outline: 0;
              color: transparent;
              caret-color: transparent;
              transition: background-color 9999s ease-in-out 0s;
              box-sizing: border-box;
              -webkit-text-fill-color: transparent;
            }
            """
                .trimIndent()
    }
}
