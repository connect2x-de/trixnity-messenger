package de.connect2x.messenger.previews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.room.timeline.element.details.FileBasedDetailsHeader
import de.connect2x.messenger.compose.view.room.timeline.element.details.FileBasedDetailsHeaderButton
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.ImageRoomMessageTimelineElementViewModelImpl
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun ElementDetailsHeaderPreview() {
    InitMessengerPreview {
        FileBasedDetailsHeader(
            element = ImageRoomMessageTimelineElementViewModelImpl(
                viewModelContext = MatrixClientViewModelContextImpl(
                    di = DI.current,
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    userId = UserId("1", "localhost"),
                ),
                content = FileBased.Image(body = "image.png")
            ),
            {},
            {},
        ) {
            FileBasedDetailsHeaderButton(
                Icons.Outlined.ZoomIn,
                "zoom in",
                {},
            )
            FileBasedDetailsHeaderButton(
                Icons.Outlined.ZoomOut,
                "zoom out",
                {},
            )
        }
    }
}
