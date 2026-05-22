package de.connect2x.trixnity.messenger.compose.view.common.modifier

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics

fun Modifier.expandable(isExpanded: MutableState<Boolean>) = this.expandable(isExpanded.value) { isExpanded.value = it }

fun Modifier.expandable(isExpanded: Boolean, setExpanded: (Boolean) -> Unit = {}) =
    this.semantics {
        if (isExpanded)
            collapse {
                setExpanded(false)
                true
            }
        else
            expand {
                setExpanded(true)
                true
            }
    }
