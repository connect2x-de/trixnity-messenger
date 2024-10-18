package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials

@Composable
fun ReactorList(
    focusRequester: FocusRequester,
    reactors: Map<String, List<UserInfoElement>>,
) {
    val i18n = DI.get<I18nView>()
    val reactions = reactors.toList()
    val reactionCounts = listOf(i18n.commonAll() to reactors.values.flatten().size) + reactions.map { it.first to it.second.size }
    var selectedReaction by remember {
        mutableStateOf(0)
    }

    Column(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusTarget()
            .fillMaxHeight()
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedReaction,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 0.dp,
            divider = {}
        ) {
            reactionCounts.forEachIndexed { index, (reaction, count) ->
                Tab(
                    selected = selectedReaction == index,
                    onClick = { selectedReaction = index },
                    Modifier.minimumInteractiveComponentSize()
                ) { Text("$reaction $count") }
            }
        }
        HorizontalDivider()
        if (selectedReaction == 0) {
            reactions.map { (reaction, users) ->
                users.map { user -> reaction to user }
            }.flatten()
        } else {
            reactions[selectedReaction - 1].let { (reaction, users) ->
                users.map { user -> reaction to user }
            }
        }.forEach { (reaction, user) ->
            ReactorListElement(user, reaction)
        }
    }
}

@Composable
fun ReactorListElement(user: UserInfoElement, reaction: String?) {
    Row(
        Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.fillMaxWidth().weight(1.0f, false),
            text = user.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        reaction?.let {
            Spacer(Modifier.size(5.dp))
            Text(
                modifier = Modifier,
                text = reaction,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
