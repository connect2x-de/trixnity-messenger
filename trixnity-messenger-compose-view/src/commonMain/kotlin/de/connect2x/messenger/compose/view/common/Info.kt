package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.i18n.I18n


@Composable
fun Info(
    readers: List<String>, focusRequester: FocusRequester
) {
    val i18n = DI.get<I18n>()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.focusRequester(focusRequester).focusTarget()
    ) {
        Box {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                Text(text = i18n.timelineElementReadBy(), style = MaterialTheme.typography.titleSmall)
                HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(scrollState).fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    readers.map {
                        Text(text = it)
                    }
                }
            }
            VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
        }
    }
}
