package de.connect2x.messenger.compose.view.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import de.connect2x.messenger.compose.view.room.timeline.element.ReactionsAndReadByInfo
import de.connect2x.trixnity.messenger.util.ReactionKey
import de.connect2x.trixnity.messenger.util.ReadReceiptsHandle.Reader
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun InfoPopup(
    isOpen: Boolean,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    readers: Collection<Reader>,
    reactors: Map<ReactionKey, Collection<StateFlow<UserInfoElement?>>>,
    modifier: Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    if (isOpen) {
        ModalBottomSheet(onDismiss, modifier, sheetState) {
            ReactionsAndReadByInfo(reactors, focusRequester, readers)
        }
    }
}
