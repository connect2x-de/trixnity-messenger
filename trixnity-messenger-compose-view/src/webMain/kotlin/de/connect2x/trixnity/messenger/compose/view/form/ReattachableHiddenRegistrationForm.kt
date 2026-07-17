package de.connect2x.trixnity.messenger.compose.view.form

import js.core.Void
import js.promise.Promise
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.uuid.Uuid
import web.autofill.AutoFillNormalField
import web.autofill.newPassword
import web.autofill.username
import web.dom.document
import web.history.history
import web.location.location
import web.scheduling.queueMicrotask

internal interface ReattachableHiddenRegistrationForm : HiddenRegistrationForm {
    fun attach()

    fun detach()
}

internal fun ReattachableHiddenRegistrationForm(uniqueId: String? = null): ReattachableHiddenRegistrationForm {
    return ReattachableHiddenRegistrationFormImpl(uniqueId = uniqueId)
}

private class ReattachableHiddenRegistrationFormImpl(uniqueId: String? = null) : ReattachableHiddenRegistrationForm {
    private val hiddenInputId = uniqueId ?: Uuid.random().toHexDashString()
    private var isAttached = false
    private var submitInProgress = false

    private val inputStyle = HiddenInputHTMLStyleElement(hiddenInputId = hiddenInputId)

    private val usernameInput =
        UsernameInputElement(name = "username", autocomplete = AutoFillNormalField.username) {
            applyHiddenInputId(hiddenInputId)
            disablePointerEvents()
            disableKeyboardEvents()
            applyPosition(right = 0, bottom = 0, width = 10, height = 10)
        }

    private val passwordInput =
        PasswordInputElement(name = "password", autocomplete = AutoFillNormalField.newPassword) {
            applyHiddenInputId(hiddenInputId)
            disablePointerEvents()
            disableKeyboardEvents()
            applyPosition(right = 10, bottom = 0, width = 10, height = 10)
        }

    private val form = UsernamePasswordFormElement(usernameInput = usernameInput, passwordInput = passwordInput)

    private val container = ContainerElement(style = inputStyle, form = form)

    override fun attach() {
        if (isAttached) return
        isAttached = true

        if (!container.isConnected) document.body.prepend(container)
    }

    override fun detach() {
        if (!isAttached) return
        isAttached = false

        if (!submitInProgress) container.remove()
    }

    override fun submit(username: String, password: String) {
        require(isAttached) { "cannot submit when not attached" }
        submitInProgress = true

        Promise.resolve()
            .flatThen { usernameInput.typeInto(username) }
            .flatThen { passwordInput.typeInto(password) }
            .flatThen { form.requestAndWaitForSubmit() }
            .flatThen { triggerPasswordSaving() }
            .finally { finalizeSubmit() }
    }

    private fun finalizeSubmit() {
        submitInProgress = false
        if (!isAttached && container.isConnected) container.remove()
    }

    private fun triggerPasswordSaving(): Promise<Void> {
        return Promise { resolve ->
            @OptIn(ExperimentalWasmJsInterop::class)
            history.replaceState(data = history.state, unused = "", url = location.href)

            usernameInput.blur()
            passwordInput.blur()

            container.removeChild(form)
            queueMicrotask {
                container.appendChild(form)
                resolve(null)
            }
        }
    }
}
