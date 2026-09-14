package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.CopyToClipboardButton
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSelectableText

interface CopyableUserId {
    @Composable fun create(userId: UserId, textStyle: TextStyle)
}

@Composable
fun CopyableUserId(userId: UserId, textStyle: TextStyle) {
    DI.get<CopyableUserId>().create(userId = userId, textStyle = textStyle)
}

class CopyableUserIdImpl : CopyableUserId {
    @Composable
    override fun create(userId: UserId, textStyle: TextStyle) {
        val i18n = DI.get<I18nView>()

        @Suppress("DEPRECATION") // TODO: New clipboard API is not usable from common code, fix this eventually..
        val clipboard = LocalClipboardManager.current

        Row(verticalAlignment = Alignment.CenterVertically) {
            ThemedSelectableText(
                userId.full,
                MaterialTheme.components.selectionOnSurface,
                style = textStyle,
                overflow = TextOverflow.Visible,
            )
            Spacer(Modifier.size(5.dp))
            CopyToClipboardButton(userId.full, i18n.userProfileCopyUserId())
        }
    }
}
