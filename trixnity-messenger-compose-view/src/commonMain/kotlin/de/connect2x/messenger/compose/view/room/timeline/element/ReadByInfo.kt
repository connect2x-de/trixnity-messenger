package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.util.ReadReceiptsRepository.ReadReceiptsHandle.Reader


@Composable
fun ReadByInfo(
    readers: Collection<Reader>,
    focusRequester: FocusRequester,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.focusRequester(focusRequester).focusTarget()
    ) {
        Box {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 10.dp)
            ) {
                readers.forEach {
                    val readerInfo = it.userInfo.collectAsState().value
                    if (readerInfo != null) Text(
                        text = readerInfo.name,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
        }
    }
}
