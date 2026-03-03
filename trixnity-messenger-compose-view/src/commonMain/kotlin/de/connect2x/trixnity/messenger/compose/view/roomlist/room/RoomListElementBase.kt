package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.common.modifier.PlaceholderHighlight
import de.connect2x.trixnity.messenger.compose.view.common.modifier.fade
import de.connect2x.trixnity.messenger.compose.view.common.placeholder
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

@Composable
fun RoomListElementBase(
    roomListElementViewModel: RoomListElementViewModel,
    roomDetails: @Composable ColumnScope.() -> Unit,
    roomActions: @Composable ColumnScope.() -> Unit
) {
    val isLoaded = roomListElementViewModel.isLoaded.collectAsState().value

    Row(modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth()) {
        MatrixClientColor(roomListElementViewModel)
        Row(
            Modifier
                .heightIn(min = 72.dp)
                .padding(top = 10.dp, bottom = 10.dp, end = 10.dp)
                .placeholder(
                    visible = !isLoaded,
                    color = Color.LightGray,
                    shape = RoundedCornerShape(8.dp),
                    highlight = PlaceholderHighlight.fade(highlightColor = Color(0xFFDDDDDD))
                ).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.clearAndSetSemantics {}) {
                RoomImage(roomListElementViewModel)
            }
            Spacer(Modifier.size(10.dp))
            Column(
                Modifier.weight(1.0f, true),
                verticalArrangement = Arrangement.Center
            ) {
                roomDetails()
            }
            Column(Modifier, verticalArrangement = Arrangement.Center) {
                roomActions()
            }
        }
    }
}
