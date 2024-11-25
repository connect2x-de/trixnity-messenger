package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import de.connect2x.messenger.compose.view.common.TimelinePopup
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement

@Composable
fun ReactorListPopup(
    isOpen: Boolean,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isByMe: Boolean,
    reactors: Map<String, List<UserInfoElement>>,
) {
    TimelinePopup(isOpen, onDismiss, modifier, isByMe) {
        ReactorList(focusRequester, reactors)
    }
}
