package de.connect2x.trixnity.messenger.compose.view.form

import js.core.Void
import js.objects.unsafeJso
import js.promise.Promise
import web.animations.requestAnimationFrame
import web.cssom.ElementCSSInlineStyle
import web.dom.Element
import web.events.CHANGE
import web.events.Event
import web.events.EventTarget
import web.events.addEventHandler
import web.focus.BLUR
import web.focus.FOCUS
import web.focus.FOCUS_IN
import web.focus.FocusEvent
import web.form.SUBMIT
import web.form.SubmitEvent
import web.html.HTMLElement
import web.html.HTMLFormElement
import web.html.HTMLInputElement
import web.input.INPUT
import web.input.InputEvent
import web.keyboard.KEY_DOWN
import web.keyboard.KEY_PRESS
import web.keyboard.KEY_UP
import web.keyboard.KeyboardEvent
import web.pointer.CLICK
import web.pointer.POINTER_CANCEL
import web.pointer.POINTER_DOWN
import web.pointer.POINTER_ENTER
import web.pointer.POINTER_LEAVE
import web.pointer.POINTER_MOVE
import web.pointer.POINTER_UP
import web.pointer.PointerEvent
import web.scheduling.queueMicrotask
import web.timers.setTimeout

internal fun Element.applyHiddenInputId(hiddenInputId: String) {
    setAttribute(HIDDEN_INPUT_ID_ATTR, hiddenInputId)
}

internal fun HTMLInputElement.forwardInputEvents(enabled: () -> Boolean = { true }, onChange: (String) -> Unit) {
    addEventHandler(InputEvent.INPUT) {
        val currentValue = value
        queueMicrotask { if (enabled()) onChange(currentValue) }
        setTimeout(
            {
                value = ""
                blur()
            },
            100,
        )
    }
}

internal fun EventTarget.forwardPointerEvents(onEvent: (AutofillPointerEvent) -> Unit) {
    addEventHandler(PointerEvent.POINTER_ENTER) { onEvent(AutofillPointerEvent.HoverEnter) }
    addEventHandler(PointerEvent.POINTER_LEAVE) { onEvent(AutofillPointerEvent.HoverExit) }
    addEventHandler(PointerEvent.POINTER_DOWN) { onEvent(AutofillPointerEvent.Press(it.x.toFloat(), it.y.toFloat())) }
    addEventHandler(PointerEvent.POINTER_UP) { onEvent(AutofillPointerEvent.Release) }
    addEventHandler(PointerEvent.POINTER_CANCEL) { onEvent(AutofillPointerEvent.Cancel) }
}

internal fun EventTarget.disableKeyboardEvents() {
    addEventHandler(KeyboardEvent.KEY_DOWN) { it.preventDefault() }
}

internal fun HTMLElement.ensureNotFocussed() {
    addEventHandler(FocusEvent.FOCUS_IN) {
        blur()
        requestAnimationFrame { blur() }
        requestAnimationFrame { requestAnimationFrame { blur() } }
    }
}

internal fun HTMLElement.forwardFocus(onFocus: () -> Unit) {
    addEventHandler(FocusEvent.FOCUS_IN) { onFocus() }
}

internal fun EventTarget.disablePointerEvents() {
    for (eventType in
        listOf(
            PointerEvent.POINTER_MOVE,
            PointerEvent.POINTER_ENTER,
            PointerEvent.POINTER_LEAVE,
            PointerEvent.POINTER_UP,
            PointerEvent.POINTER_DOWN,
            PointerEvent.CLICK,
        )) {
        addEventHandler(eventType, unsafeJso { capture = true }) {
            it.preventDefault()
            it.stopImmediatePropagation()
        }
    }
}

internal fun HTMLFormElement.requestAndWaitForSubmit(): Promise<Void> {
    return Promise { resolve ->
        addEventHandler(SubmitEvent.SUBMIT, unsafeJso { once = true }) {
            it.preventDefault()
            resolve(null)
        }
        requestSubmit()
    }
}

internal fun ElementCSSInlineStyle.applyPosition(
    left: Int? = null,
    top: Int? = null,
    right: Int? = null,
    bottom: Int? = null,
    width: Int? = null,
    height: Int? = null,
) {
    style.apply {
        this.left = left.toPx()
        this.top = top.toPx()
        this.right = right.toPx()
        this.bottom = bottom.toPx()
        this.width = width.toPx()
        this.height = height.toPx()
    }
}

private fun Int?.toPx(): String = this?.let { "${it}px" } ?: ""

internal fun HTMLInputElement.typeInto(text: String): Promise<Void> {
    val element = this

    return macroTask()
        .flatThen { typeTextIntoInput(element, text) }
        .flatThen { finalizeTypingIntoInput(element, text) }
        .flatThen { focusInput(element) }
}

private fun focusInput(element: HTMLInputElement): Promise<Void> {
    element.focus()
    element.dispatchEvent(FocusEvent(bubbles = true))

    return macroTask()
}

private fun typeTextIntoInput(element: HTMLInputElement, text: String): Promise<Void> {
    return text.indices.fold(macroTask()) { promise, index ->
        val currentChar = text.substring(index, index + 1)
        val currentRange = text.substring(0, index + 1)

        promise.flatThen { typeCharIntoInput(element, currentChar, currentRange) }
    }
}

private fun typeCharIntoInput(element: HTMLInputElement, currentChar: String, currentRange: String): Promise<Void> {
    element.dispatchEvent(KeyDownEvent(bubbles = true, key = currentChar))
    element.dispatchEvent(KeyPressEvent(bubbles = true, key = currentChar))

    element.value = currentRange

    element.dispatchEvent(KeyUpEvent(bubbles = true))

    return macroTask()
}

private fun finalizeTypingIntoInput(element: HTMLInputElement, text: String): Promise<Void> {
    element.value = text

    element.dispatchEvent(InputEvent(bubbles = true, inputType = "insertText", data = text))
    element.dispatchEvent(ChangeEvent(bubbles = true))

    return macroTask()
}

private fun macroTask(): Promise<Void> {
    return Promise { resolve -> setTimeout({ resolve(null) }) }
}

private fun InputEvent(bubbles: Boolean? = null, inputType: String? = null, data: String? = null): InputEvent {
    return InputEvent(
        type = InputEvent.INPUT,
        init =
            unsafeJso {
                this.bubbles = bubbles
                this.inputType = inputType
                this.data = data
            },
    )
}

private fun ChangeEvent(bubbles: Boolean? = null): Event {
    return Event(type = Event.CHANGE, init = unsafeJso { this.bubbles = bubbles })
}

private fun FocusEvent(bubbles: Boolean? = null): FocusEvent {
    return FocusEvent(type = FocusEvent.FOCUS, init = unsafeJso { this.bubbles = bubbles })
}

private fun BlurEvent(bubbles: Boolean? = null): FocusEvent {
    return FocusEvent(type = FocusEvent.BLUR, init = unsafeJso { this.bubbles = bubbles })
}

private fun KeyDownEvent(bubbles: Boolean? = null, key: String? = null): KeyboardEvent {
    return KeyboardEvent(
        type = KeyboardEvent.KEY_DOWN,
        init =
            unsafeJso {
                this.bubbles = bubbles
                this.key = key
            },
    )
}

private fun KeyPressEvent(bubbles: Boolean? = null, key: String? = null): KeyboardEvent {
    return KeyboardEvent(
        type = KeyboardEvent.KEY_PRESS,
        init =
            unsafeJso {
                this.bubbles = bubbles
                this.key = key
            },
    )
}

private fun KeyUpEvent(bubbles: Boolean? = null): KeyboardEvent {
    return KeyboardEvent(type = KeyboardEvent.KEY_UP, init = unsafeJso { this.bubbles = bubbles })
}
