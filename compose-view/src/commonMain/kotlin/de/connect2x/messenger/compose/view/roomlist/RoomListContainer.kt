package de.connect2x.messenger.compose.view.roomlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.ErrorDialog
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.roomlist.header.AccountData
import de.connect2x.messenger.compose.view.roomlist.header.NotVerifiedBanner
import de.connect2x.messenger.compose.view.roomlist.header.SearchRoomsBanner
import de.connect2x.messenger.compose.view.roomlist.header.SpacesBanner
import de.connect2x.messenger.compose.view.roomlist.header.SyncErrorBanner
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.ErrorType


interface RoomListContainerView {
    @Composable
    fun create(roomListViewModel: RoomListViewModel)
}

@Composable
fun RoomListContainer(roomListViewModel: RoomListViewModel) {
    DI.current.get<RoomListContainerView>().create(roomListViewModel)
}

class RoomListContainerViewImpl : RoomListContainerView {
    @Composable
    override fun create(roomListViewModel: RoomListViewModel) {
        val i18n = DI.current.get<I18nView>()
        val error = roomListViewModel.error.collectAsState().value
        Box {
            if (error != null) {
                ErrorDialog(
                    error,
                    { roomListViewModel.errorDismiss() },
                    errorCause = if (roomListViewModel.errorType.value == ErrorType.WITH_ACTION) i18n.roomListRemoveRoom() else null,
                )
            }
            Column {
                AccountData(roomListViewModel)
                HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                NotVerifiedBanner(roomListViewModel)
                SyncErrorBanner(roomListViewModel)
                SearchRoomsBanner(roomListViewModel)
                SpacesBanner(roomListViewModel)
                RoomList(roomListViewModel)
            }
        }
    }
}


