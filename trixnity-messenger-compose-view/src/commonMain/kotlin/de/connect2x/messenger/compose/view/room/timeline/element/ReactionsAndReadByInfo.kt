package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey

@Composable
fun ReactionsAndReadByInfo(
    reactors: Map<ReactionKey, Collection<UserInfoElement>>,
    focusRequester: FocusRequester,
    readers: Collection<UserInfoElement>,
) {
    val i18n = DI.get<I18nView>()
    val selectedTab = remember { mutableStateOf(0) }

    Column {
        TabRow(
            selectedTabIndex = selectedTab.value,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Tab(
                selected = selectedTab.value == 0,
                onClick = { selectedTab.value = 0 },
                Modifier.minimumInteractiveComponentSize()
            ) { Text(i18n.messageInfoReactions(), modifier = Modifier.padding(horizontal = 10.dp)) }
            Tab(
                selected = selectedTab.value == 1,
                onClick = { selectedTab.value = 1 },
                Modifier.minimumInteractiveComponentSize()
            ) { Text(i18n.messageInfoReadBy(), modifier = Modifier.padding(horizontal = 10.dp)) }
        }
        when (selectedTab.value) {
            0 -> ReactorList(reactors, focusRequester)
            1 -> ReadByInfo(readers, focusRequester)
        }
    }
}
