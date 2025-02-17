package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import de.connect2x.messenger.compose.view.room.timeline.element.ReactionsAndReadByInfo
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey
import de.connect2x.trixnity.messenger.viewmodel.util.ReadReceiptsHandle.Reader

@Composable
actual fun InfoPopup(
    isOpen: Boolean,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    readers: Collection<Reader>,
    reactors: Map<ReactionKey, Collection<UserInfoElement>>,
    modifier: Modifier
) {
    if (isOpen) {
        Popup(
            onDismissRequest = onDismiss,
            alignment = Alignment.BottomEnd,
            properties = PopupProperties(
                focusable = true,
            ),
        ) {
            Surface(
                Modifier.size(320.dp, 400.dp),
                shadowElevation = 4.dp,
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                ReactionsAndReadByInfo(reactors, focusRequester, readers)
            }
        }
    }
}
