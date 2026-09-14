package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSelectableText
import de.connect2x.trixnity.messenger.compose.view.util.UserIdText

interface SettingsAccountCard {
    @Composable fun create(userId: UserId, modifier: Modifier, content: @Composable ColumnScope.() -> Unit)
}

@Composable
fun SettingsAccountCard(userId: UserId, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    DI.get<SettingsAccountCard>().create(userId, modifier, content)
}

class SettingsAccountCardImpl : SettingsAccountCard {
    @Composable
    override fun create(userId: UserId, modifier: Modifier, content: @Composable (ColumnScope.() -> Unit)) {
        ElevatedCard(modifier.padding(bottom = 10.dp)) {
            Column(Modifier.padding(10.dp)) {
                UserIdText(
                    userId = userId,
                    showTooltip = false,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.focusHighlighting(),
                )
                HorizontalDivider(Modifier.fillMaxWidth().padding(vertical = 10.dp))
                content()
            }
        }
    }
}

@Composable
fun SettingsAccountCardWithAdditionalButtons(
    userId: UserId,
    buttons: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(Modifier.padding(bottom = 10.dp)) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f, true).fillMaxWidth()) {
                    ThemedSelectableText(
                        text = userId.full,
                        selectionStyle = MaterialTheme.components.selectionOnSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                buttons()
            }
            HorizontalDivider(Modifier.fillMaxWidth().padding(vertical = 10.dp))
            content()
        }
    }
}
