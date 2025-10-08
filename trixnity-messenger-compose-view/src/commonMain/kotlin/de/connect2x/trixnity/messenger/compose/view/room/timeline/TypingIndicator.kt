package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.isMobile
import de.connect2x.trixnity.messenger.compose.view.common.TypingIndicator
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel

interface TypingIndicatorView {
    @Composable
    fun create(timelineViewModel: TimelineViewModel)
}

@Composable
fun TypingIndicator(timelineViewModel: TimelineViewModel) {
    with(DI.get<TypingIndicatorView>()) { create(timelineViewModel) }
}

class TypingIndicatorViewImpl : TypingIndicatorView {
    @Composable
    override fun create(timelineViewModel: TimelineViewModel) {
        val typing = timelineViewModel.roomHeaderViewModel.usersTyping.collectAsState().value
        val isDirect = timelineViewModel.isDirect.collectAsState().value
        val roomHeaderInfo = timelineViewModel.roomHeaderViewModel.roomHeaderInfo.collectAsState().value
        if (Platform.current.isMobile.not()) {
            Box(Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
                if (typing == null) {
                    Box(Modifier.heightIn(min = 20.dp, max = 20.dp))
                } else {
                    // in the header, the room name is directly above, here we have to fetch this info
                    val typingText = if (isDirect) "${roomHeaderInfo.roomName} $typing" else typing
                    Tooltip({ Text(typingText) }) {
                        Box(Modifier.heightIn(min = 20.dp, max = 20.dp).widthIn(min = 80.dp, max = 80.dp)) {
                            TypingIndicator(
                                "",
                                style = MaterialTheme.typography.titleLarge,
                                cycleDuration = 1_500
                            )
                        }
                    }
                }
            }
        }
    }
}
