package de.connect2x.trixnity.messenger.compose.view.space

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.HeaderSurface
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.modifier.minHeaderHeight
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedHorizontalDivider
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.util.GetAccountProfileDisplayName

@Composable
internal fun SpaceHeader(
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.messengerDpConstants.small),
    content: @Composable RowScope.() -> Unit,
) {
    HeaderSurface {
        Column {
            Row(
                Modifier.fillMaxWidth().minHeaderHeight().padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small),
                content = content,
            )
            ThemedHorizontalDivider(Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun SpaceSectionHeader(title: String) {
    Text(
        title,
        Modifier.fillMaxWidth().padding(MaterialTheme.messengerDpConstants.middle),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun SpaceAccountName(userId: UserId?) {
    val accounts by DI.get<MatrixClients>().collectAsState()
    if (accounts.size <= 1 || userId == null) return
    val displayNames = DI.get<GetAccountProfileDisplayName>()
    val name by remember(displayNames, userId) { displayNames.fromUserId(userId) }.collectAsState(userId.localpart)
    Text(
        name.ifBlank { userId.localpart },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun SpaceBackButton(onBack: () -> Unit) {
    val i18n = DI.get<I18nView>()
    Tooltip(tooltip = { Text(i18n.commonBack()) }) {
        ThemedIconButton(
            style = MaterialTheme.components.commonIconButton,
            onClick = onBack,
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = i18n.commonBack() },
        ) {
            Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, contentDescription = null)
        }
    }
}
