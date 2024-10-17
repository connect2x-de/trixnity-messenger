package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement

@Composable
fun ReactorList(
    focusRequester: FocusRequester,
    reactors: Map<String, List<UserInfoElement>>,
) {
    val scrollState = rememberScrollState()
    val reactions = reactors.keys.toList()
    var selectedReaction by remember {
        mutableStateOf(0)
    }

    Column(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusTarget()
    ) {
        TabRow(selectedReaction) {
            Tab(
                selected = selectedReaction == 0,
                onClick = { selectedReaction = 0 },
            ) { Text("All") }
            reactions.forEachIndexed { index, reaction ->
                Tab(
                    selected = selectedReaction == index + 1,
                    onClick = { selectedReaction = index + 1 },
                ) { Text(reaction) }
            }
        }
        if (selectedReaction == 0) {
            reactors.values.flatten().forEach { user ->
                Text(user.userId.full)
            }
        } else {
            reactors[reactions[selectedReaction - 1 ]]?.forEach { user ->
                Text(user.userId.full)
            }
        }
    }
}
