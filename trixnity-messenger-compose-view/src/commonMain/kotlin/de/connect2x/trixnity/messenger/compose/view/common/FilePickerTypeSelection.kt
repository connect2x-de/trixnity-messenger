package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.messengerIcons


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
                            FilePickerType.ATTACHMENT_FILE -> {
                                Tooltip({ Text(i18nView.fileDialogLoadFileButton()) }) {
                                    ThemedIconButton(
                                        style = MaterialTheme.components.commonIconButton,
                                        onClick = { onSelect(pickerType) },
                                    ) {
                                        Icon(
                                            MaterialTheme.messengerIcons.attachFile,
                                            i18nView.fileDialogLoadFileButton()
                                        )
                                    }
                                }
                            }

                            FilePickerType.IMAGE_FILE ->
                                Tooltip({ Text(i18nView.fileDialogLoadImageButton()) }) {
                                    ThemedIconButton(
                                        style = MaterialTheme.components.commonIconButton,
                                        onClick = { onSelect(pickerType) },
                                    ) {
                                        Icon(
                                            MaterialTheme.messengerIcons.attachImage,
                                            i18nView.fileDialogLoadImageButton()
                                        )
                                    }
                                }

                            FilePickerType.IMAGE_AND_VIDEO_FILE ->
                                Tooltip({ Text(i18nView.fileDialogLoadImageOrVideoButton()) }) {
                                    ThemedIconButton(
                                        style = MaterialTheme.components.commonIconButton,
                                        onClick = { onSelect(pickerType) },
                                    ) {
                                        Icon(
                                            MaterialTheme.messengerIcons.attachImage,
                                            i18nView.fileDialogLoadImageOrVideoButton()
                                        )
                                    }
                                }

                            FilePickerType.PHOTO_CAPTURE ->
                                Tooltip({ Text(i18nView.fileDialogTakeImageButton()) }) {
                                    ThemedIconButton(
                                        style = MaterialTheme.components.commonIconButton,
                                        onClick = { onSelect(pickerType) },
                                    ) {
                                        Icon(
                                            MaterialTheme.messengerIcons.recordPhoto,
                                            i18nView.fileDialogTakeImageButton()
                                        )
                                    }
                                }

                            FilePickerType.VIDEO_CAPTURE ->
                                Tooltip({ Text(i18nView.fileDialogTakeVideoButton()) }) {
                                    ThemedIconButton(
                                        style = MaterialTheme.components.commonIconButton,
                                        onClick = { onSelect(pickerType) },
                                    ) {
                                        Icon(
                                            MaterialTheme.messengerIcons.recordVideo,
                                            i18nView.fileDialogTakeVideoButton()
                                        )
                                    }
                                }
                        }
                    }
                }
            }
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
