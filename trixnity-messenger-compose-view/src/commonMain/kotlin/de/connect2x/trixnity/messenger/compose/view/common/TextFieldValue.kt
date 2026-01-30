package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.ui.text.input.TextFieldValue

fun TextFieldValue.maxLength(maxLength: Int): TextFieldValue = copy(text.take(maxLength))
