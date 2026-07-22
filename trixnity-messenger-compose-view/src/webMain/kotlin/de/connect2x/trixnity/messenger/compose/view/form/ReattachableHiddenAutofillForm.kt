package de.connect2x.trixnity.messenger.compose.view.form

import kotlin.uuid.Uuid
import web.autofill.AutoFillNormalField
import web.autofill.currentPassword
import web.autofill.username
import web.dom.document

internal interface ReattachableHiddenAutofillForm : HiddenAutofillForm {
    fun attach()

    fun detach()
}

internal sealed interface AutofillPointerEvent {
    data object HoverEnter : AutofillPointerEvent

    data object HoverExit : AutofillPointerEvent

    data class Press(val x: Float, val y: Float) : AutofillPointerEvent

    data object Release : AutofillPointerEvent

    data object Cancel : AutofillPointerEvent
}

internal fun ReattachableHiddenAutofillForm(
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPointerEvent: (AutofillPointerEvent) -> Unit,
    uniqueId: String? = null,
): ReattachableHiddenAutofillForm {
    return ReattachableHiddenAutofillFormImpl(
        onUsernameChange = onUsernameChange,
        onPasswordChange = onPasswordChange,
        onPointerEvent = onPointerEvent,
        uniqueId = uniqueId,
    )
}

private class ReattachableHiddenAutofillFormImpl(
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPointerEvent: (AutofillPointerEvent) -> Unit,
    uniqueId: String? = null,
) : ReattachableHiddenAutofillForm {
    private var isAttached = false
    private var wasFocused = false
    private val hiddenInputId = uniqueId ?: Uuid.random().toHexDashString()
    private val inputStyle = HiddenInputHTMLStyleElement(hiddenInputId = hiddenInputId)

    private val usernameInput =
        UsernameInputElement(name = "username", autocomplete = AutoFillNormalField.username) {
            style.borderRadius = "9999px"
            style.cursor = "pointer"

            applyHiddenInputId(hiddenInputId)
            forwardInputEvents(::shouldForwardInputs, onUsernameChange)
            forwardPointerEvents(onPointerEvent)
            disableKeyboardEvents()
            forwardFocus { wasFocused = true }
        }

    private val passwordInput =
        PasswordInputElement(name = "password", autocomplete = AutoFillNormalField.currentPassword) {
            style.borderRadius = "9999px"
            style.cursor = "default"

            applyHiddenInputId(hiddenInputId)
            forwardInputEvents(::shouldForwardInputs, onPasswordChange)
            disablePointerEvents()
            disableKeyboardEvents()
            ensureNotFocussed()
        }

    private val form = UsernamePasswordFormElement(usernameInput = usernameInput, passwordInput = passwordInput)

    private val container = ContainerElement(style = inputStyle, form = form)

    override fun attach() {
        isAttached = true
        document.body.prepend(container)
    }

    override fun detach() {
        isAttached = false
        wasFocused = false
        container.remove()
    }

    override fun layoutUsername(left: Int, top: Int, width: Int, height: Int) {
        usernameInput.applyPosition(left = left, top = top, width = width, height = height)
    }

    override fun layoutPassword(left: Int, top: Int, width: Int, height: Int) {
        passwordInput.applyPosition(left = left, top = top, width = width, height = height)
    }

    private fun shouldForwardInputs(): Boolean {
        return isAttached && wasFocused
    }
}
