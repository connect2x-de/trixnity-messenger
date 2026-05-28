package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedFloatingActionButton
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel

@Composable
fun BoxScope.CreateNewGroupButton(createNewGroupViewModel: CreateNewGroupViewModel) {
    val i18n = DI.get<I18nView>()
    val canCreateNewGroup by createNewGroupViewModel.canCreateNewGroup.collectAsState()
    val isCreating by createNewGroupViewModel.isCreating.collectAsState()

    Box(Modifier.align(Alignment.BottomEnd).padding(bottom = 20.dp, end = 20.dp)) {
        ThemedFloatingActionButton(
            expanded = true,
            enabled = isCreating.not() && canCreateNewGroup,
            onClick = { createNewGroupViewModel.createNewGroup() },
            text = { Text(i18n.createNewGroupCreate()) },
            icon = { Icon(Icons.Default.Check, i18n.createNewGroupCreate()) },
        )
    }
}
