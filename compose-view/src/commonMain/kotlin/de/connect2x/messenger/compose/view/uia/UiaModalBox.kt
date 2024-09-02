package de.connect2x.messenger.compose.view.uia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.blockPointerInput


interface UiaModalBoxView {
    @Composable
    fun create(content: @Composable BoxScope.() -> Unit)
}

@Composable
fun UiaModalBox(content: @Composable BoxScope.() -> Unit) {
    DI.current.get<UiaModalBoxView>().create(content)
}

class UiaModalBoxViewImpl : UiaModalBoxView {
    @Composable
    override fun create(content: @Composable BoxScope.() -> Unit) {
        BoxWithConstraints(
            Modifier.fillMaxSize()
                .blockPointerInput()
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .width(min(420.dp, maxWidth - 16.dp))
                    .background(MaterialTheme.colorScheme.background),
                content = content,
            )
        }
    }
}