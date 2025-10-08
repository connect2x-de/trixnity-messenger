package de.connect2x.trixnity.messenger.compose.view.sharing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.files.toImageBitmap
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.roomlist.room.RoomListElementContainer
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogScrollContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedAdaptiveDialog
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.SharedData
import de.connect2x.trixnity.messenger.viewmodel.sharing.ShareDataViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import net.folivo.trixnity.utils.toByteArray

interface ShareDataView {
    @Composable
    fun create(viewModel: ShareDataViewModel)
}

@Composable
fun ShareData(viewModel: ShareDataViewModel) {
    DI.get<ShareDataView>().create(viewModel)
}

class ShareDataViewImpl : ShareDataView {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun create(viewModel: ShareDataViewModel) {
        val i18n = DI.get<I18nView>()
        val state = rememberLazyListState()
        val initialSyncFinished by viewModel.roomList.initialSyncFinished.collectAsState()
        val allRooms by viewModel.roomList.elements.collectAsState()
        val selectedRoomId by viewModel.selectedRoomId.collectAsState()
        val sending by viewModel.sending.collectAsState()
        val enabled = selectedRoomId != null && !sending
        val maxMediaSize = DI.get<MatrixMessengerConfiguration>().maxMediaSizeInMemory

        LaunchedEffect(initialSyncFinished) {
            state.scrollToItem(0)
        }
        LaunchedEffect(allRooms) {
            if (state.layoutInfo.visibleItemsInfo.any { it.index == 1 }) { // this has been the first element before
                state.animateScrollToItem(0)
            }
        }

        ThemedAdaptiveDialog(viewModel::cancel) {
            AdaptiveDialogHeader(onClose = viewModel::cancel) {
                Text(i18n.shareDataTitle(viewModel.sharedData))
            }
            AdaptiveDialogScrollContent(scrollState = state) {
                when (val data = viewModel.sharedData) {
                    is SharedData.SingleFile -> ShareFilesLazyRow(listOf(data.file))
                    is SharedData.MultipleFiles -> ShareFilesLazyRow(data.files)
                    is SharedData.PlainText -> ShareTextRow(data.text)
                    is SharedData.Url -> ShareUrlRow(data.url, data.icon, maxMediaSize)
                }
                if (allRooms.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(i18n.roomListNoRoom())
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), state) {
                        items(
                            allRooms, { it.roomId.full }) { roomListElement ->
                            RoomListElementContainer(
                                roomListElement.roomId,
                                viewModel.roomList,
                                roomListElement,
                            )
                        }
                    }
                }
            }
            AdaptiveDialogFooter {
                ThemedButton(
                    style = MaterialTheme.components.commonButton,
                    onClick = viewModel::cancel,
                ) {
                    Text(i18n.actionCancel())
                }
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    onClick = viewModel::send,
                    enabled = enabled
                ) {
                    Text(i18n.inputAreaSend())
                }
            }
        }
    }
}

private fun splitFilename(name: String): Pair<String, String?> {
    val index = name.lastIndexOf('.')
    if (index < 0) return Pair(name, null)
    return Pair(name.substring(0, index), name.substring(index))
}

@Composable
private fun ShareTextRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.padding(horizontal = MaterialTheme.messengerDpConstants.small)
    ) {
        Card(Modifier.height(MaterialTheme.messengerDpConstants.touchTarget).fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight().padding(
                    start = MaterialTheme.messengerDpConstants.small,
                    end = MaterialTheme.messengerDpConstants.middle
                )
            ) {
                Text(
                    text, style = MaterialTheme.typography.bodyLarge, softWrap = false, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ShareUrlRow(text: String, icon: FileDescriptor?, maxMediaSize: Long) {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(icon) {
        icon?.content?.toByteArray(maxMediaSize)
            ?.also { image = it.toImageBitmap() }
    }

    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.padding(horizontal = MaterialTheme.messengerDpConstants.small)
    ) {
        Card(Modifier.height(MaterialTheme.messengerDpConstants.touchTarget).fillMaxWidth()) {
            Row {
                Box(
                    modifier = Modifier.aspectRatio(1.0f).fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    image?.let {
                        Image(
                            it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } ?: run {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight().padding(
                        start = MaterialTheme.messengerDpConstants.small,
                        end = MaterialTheme.messengerDpConstants.middle
                    )
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyLarge,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareFilesLazyRow(files: List<FileDescriptor>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MaterialTheme.messengerDpConstants.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small),
    ) {
        items(files) { file ->
            ShareFileCard(file)
        }
    }
}

@Composable
private fun ShareFileCard(file: FileDescriptor) {
    val i18n = DI.get<I18nView>()
    val fileSize = "(" + (file.fileSize?.let { size -> formatSize(size) } ?: i18n.commonUnknown()) + ")"
    val isImage = file.mimeType?.match("image/*") == true
    val isVideo = file.mimeType?.match("video/*") == true
    val isAudio = file.mimeType?.match("audio/*") == true
    val (baseName, fileExtension) = splitFilename(file.fileName)
    Card(Modifier.height(MaterialTheme.messengerDpConstants.touchTarget)) {
        Row {
            Box(
                modifier = Modifier.aspectRatio(1.0f).fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(
                    when {
                        isImage -> MaterialTheme.messengerIcons.typeImage
                        isVideo -> MaterialTheme.messengerIcons.typeVideo
                        isAudio -> MaterialTheme.messengerIcons.typeAudio
                        else -> MaterialTheme.messengerIcons.typeFile
                    },
                    null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
            Column(
                verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight().padding(
                    start = MaterialTheme.messengerDpConstants.small,
                    end = MaterialTheme.messengerDpConstants.middle
                ).widthIn(min = MaterialTheme.messengerDpConstants.touchTarget * 2)
            ) {
                Row {
                    Text(
                        baseName,
                        modifier = Modifier.widthIn(max = MaterialTheme.messengerDpConstants.touchTarget * 3),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (fileExtension != null) {
                        Text(
                            fileExtension,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            softWrap = false
                        )
                    }
                }
                Text(
                    fileSize, style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
