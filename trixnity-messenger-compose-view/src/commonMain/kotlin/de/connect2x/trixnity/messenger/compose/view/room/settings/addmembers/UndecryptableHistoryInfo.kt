package de.connect2x.trixnity.messenger.compose.view.room.settings.addmembers

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModel

@Composable
fun UndecryptableHistoryInfo(addMembersViewModel: AddMembersViewModel) {
    val i18n = DI.get<I18nView>()
    val undecryptableHistoryInfo = addMembersViewModel.undecryptableHistoryInfo.collectAsState().value

    if (undecryptableHistoryInfo != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(all = 15.dp).focusProperties { canFocus = false },
        ) {
            Icon(Icons.Default.Info, i18n.commonInformation())
            MiddleSpacer()
            Text(undecryptableHistoryInfo)
        }
        HorizontalDivider()
    }
}
