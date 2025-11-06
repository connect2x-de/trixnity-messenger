package de.connect2x.messenger.compose.view.uia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get


interface UiaModalBoxView {
    @Composable
    fun create(content: @Composable BoxScope.() -> Unit)
}

@Composable
fun UiaModalBox(content: @Composable BoxScope.() -> Unit) {
    DI.get<UiaModalBoxView>().create(content)
}

class UiaModalBoxViewImpl : UiaModalBoxView {
    @Composable
    override fun create(content: @Composable BoxScope.() -> Unit) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                content = content
            )
        }
    }
}
