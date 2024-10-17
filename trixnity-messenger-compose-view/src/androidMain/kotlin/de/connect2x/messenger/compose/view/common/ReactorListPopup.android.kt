package de.connect2x.messenger.compose.view.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ReactorListPopup(
    isOpen: Boolean,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    modifier: Modifier,
    isByMe: Boolean,
    reactors: Map<String, List<UserInfoElement>>,
    ) {
    val sheetState = rememberModalBottomSheetState()
    if (isOpen) {
        ModalBottomSheet(onDismiss, modifier, sheetState) {
            ReactorList(focusRequester, reactors)
        }
    }
}
