package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.SINGLE_PANE_THRESHOLD
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.theme.messengerDpConstants

interface AdaptiveDialog {
    @Composable
    fun create(onDismissRequest: () -> Unit, content: @Composable () -> Unit)
}

@Composable
fun AdaptiveDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    DI.get<AdaptiveDialog>().create(onDismissRequest, content)
}

// Full screen on mobile, separate dialog on larger screens
class AdaptiveDialogImpl : AdaptiveDialog {
    @Composable
    override fun create(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isSinglePane = this@BoxWithConstraints.maxWidth < SINGLE_PANE_THRESHOLD.dp
            val maxContentHeight = min(1200.dp, maxHeight - (MaterialTheme.messengerDpConstants.large * 2))
            val maxContentWidth = 800.dp
            Dialog(
                onDismissRequest = onDismissRequest,
                properties = DialogProperties(usePlatformDefaultWidth = !isSinglePane),
            ) {
                Box(
                    if (isSinglePane) Modifier
                    else Modifier.sizeIn(maxWidth = maxContentWidth, maxHeight = maxContentHeight)
                ) {
                    Surface(
                        Modifier.fillMaxSize(),
                        if (isSinglePane) RectangleShape
                        else RoundedCornerShape(MaterialTheme.messengerDpConstants.small),
                        shadowElevation = 6.dp
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
