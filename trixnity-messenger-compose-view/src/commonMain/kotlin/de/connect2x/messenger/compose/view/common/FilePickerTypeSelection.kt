package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.FilePickerType.ATTACHMENT_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.IMAGE_AND_VIDEO_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.IMAGE_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.PHOTO_CAPTURE
import de.connect2x.messenger.compose.view.common.FilePickerType.VIDEO_CAPTURE
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerIcons


interface FilePickerTypeSelectionView {
    @Composable
    fun create(
        availableTypes: List<FilePickerType>,
        onSelect: (FilePickerType) -> Unit,
        onDismiss: () -> Unit,
    )
}

@Composable
fun FilePickerTypeSelection(
    availableTypes: List<FilePickerType>,
    onSelect: (FilePickerType) -> Unit,
    onDismiss: () -> Unit,
) {
    DI.get<FilePickerTypeSelectionView>().create(
        availableTypes, onSelect, onDismiss,
    )
}

class FilePickerTypeSelectionViewImpl : FilePickerTypeSelectionView {
    @Composable
    override fun create(
        availableTypes: List<FilePickerType>,
        onSelect: (FilePickerType) -> Unit,
        onDismiss: () -> Unit,
    ) {
        val i18nView = DI.get<I18nView>()
        val offsetY = with(LocalDensity.current) { -(98.dp).roundToPx() }
        Popup(
            alignment = Alignment.CenterEnd,
            offset = IntOffset(0, offsetY),
            onDismissRequest = onDismiss,
        ) {
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    availableTypes.forEach { pickerType ->
                        when (pickerType) {
                            ATTACHMENT_FILE -> UploadButton(
                                imageVector = MaterialTheme.messengerIcons.attachFile,
                                contentDescription = i18nView.fileDialogLoadFileButton(),
                                iconButtonClick = { onSelect(pickerType) },
                            )

                            IMAGE_FILE -> UploadButton(
                                imageVector = MaterialTheme.messengerIcons.attachImage,
                                contentDescription = i18nView.fileDialogLoadImageButton(),
                                iconButtonClick = { onSelect(pickerType) },
                            )

                            IMAGE_AND_VIDEO_FILE -> UploadButton(
                                imageVector = MaterialTheme.messengerIcons.attachImage,
                                contentDescription = i18nView.fileDialogLoadImageOrVideoButton(),
                                iconButtonClick = { onSelect(pickerType) },
                            )

                            PHOTO_CAPTURE -> UploadButton(
                                imageVector = MaterialTheme.messengerIcons.recordPhoto,
                                contentDescription = i18nView.fileDialogTakeImageButton(),
                                iconButtonClick = { onSelect(pickerType) },
                            )

                            VIDEO_CAPTURE -> UploadButton(
                                imageVector = MaterialTheme.messengerIcons.recordVideo,
                                contentDescription = i18nView.fileDialogTakeVideoButton(),
                                iconButtonClick = { onSelect(pickerType) },
                                iconScale = 1.33f,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadButton(
    imageVector: ImageVector,
    contentDescription: String,
    iconButtonClick: () -> Unit,
    iconScale: Float = 1f,
) {
    IconButton(
        onClick = iconButtonClick,
        Modifier
            .size(60.dp)
            .clip(CircleShape)
    ) {
        Box(Modifier.fillMaxSize()) {
            Icon(
                imageVector,
                contentDescription,
                Modifier
                    .align(Alignment.Center)
                    .size((24 * iconScale).dp),
            )
        }
    }
}

enum class FilePickerType {
    ATTACHMENT_FILE,
    IMAGE_FILE,
    IMAGE_AND_VIDEO_FILE,
    PHOTO_CAPTURE,
    VIDEO_CAPTURE,
}
