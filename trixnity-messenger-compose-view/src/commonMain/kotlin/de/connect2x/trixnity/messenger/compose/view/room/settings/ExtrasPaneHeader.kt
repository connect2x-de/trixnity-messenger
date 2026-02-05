package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.common.ErrorView
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.HeaderBackButtonType


@Composable
fun ExtrasPaneHeader(
    title: String,
    error: String?,
    onBack: () -> Unit,
    backButtonType: HeaderBackButtonType,
    additionalButtons: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier.fillMaxSize(),
    ) {
        Column {
            Header(
                onBack, title,
                backButtonType = backButtonType,
                additionalButtons = additionalButtons,
            )
            if (error != null) ErrorView(error)
            content()
        }
    }
}
