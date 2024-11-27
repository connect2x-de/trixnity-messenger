package de.connect2x.messenger.compose.view.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.AdaptiveDialog
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.roomlist.room.RoomListElementContainer
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.sharing.ShareFilesViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize

interface ShareFilesView {
    @Composable
    fun create(viewModel: ShareFilesViewModel)
}

@Composable
fun ShareFiles(viewModel: ShareFilesViewModel) {
    DI.get<ShareFilesView>().create(viewModel)
}

class ShareFilesViewImpl : ShareFilesView {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun create(viewModel: ShareFilesViewModel) {
        val i18n = DI.get<I18nView>()
        val state = rememberLazyListState()
        val initialSyncFinished by viewModel.roomList.initialSyncFinished.collectAsState()
        val allRooms by viewModel.roomList.sortedRoomListElementViewModels.collectAsState()
        val selectedRoomId by viewModel.selectedRoomId.collectAsState()
        val sending by viewModel.sending.collectAsState()
        val enabled = selectedRoomId != null && !sending

        LaunchedEffect(initialSyncFinished) {
            state.scrollToItem(0)
        }
        LaunchedEffect(allRooms) {
            if (state.layoutInfo.visibleItemsInfo.any { it.index == 1 }) { // this has been the first element before
                state.animateScrollToItem(0)
            }
        }

        AdaptiveDialog(viewModel::cancel) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            i18n.shareFilesTitle(viewModel.sharedFiles.size),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        ToolbarButton(viewModel::cancel, i18n.shareFilesCancel()) {
                            Icon(
                                Icons.Default.Close,
                                i18n.shareFilesCancel()
                            )
                        }
                    },
                    actions = {
                        ToolbarButton(viewModel::send, i18n.inputAreaSend(), enabled) {
                            if (sending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Default.Send,
                                    i18n.inputAreaSend(),
                                    tint = if (enabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                        }
                    }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MaterialTheme.messengerDpConstants.small),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small),
                ) {
                    items(viewModel.sharedFiles) { file ->
                        ShareFileCard(file)
                    }
                }
                Spacer(Modifier.height(MaterialTheme.messengerDpConstants.small))
                Box(Modifier.fillMaxSize()) {
                    if (allRooms.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(Modifier.padding(horizontal = MaterialTheme.messengerDpConstants.middle)) {
                                Text(i18n.roomListNoRoom())
                            }
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), state) {
                            items(
                                allRooms,
                                { (roomId, _) -> roomId.full }
                            ) { roomListElement ->
                                RoomListElementContainer(
                                    roomListElement.roomId,
                                    viewModel.roomList,
                                    roomListElement.viewModel,
                                )
                            }
                        }
                    }

                    VerticalScrollbar(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        state,
                        false,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarButton(
    onClick: () -> Unit,
    description: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(content = { Text(description) })
        },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            content()
        }
    }
}

private fun splitFilename(name: String): Pair<String, String?> {
    val index = name.lastIndexOf('.')
    if (index < 0) return Pair(name, null)
    return Pair(name.substring(0, index), name.substring(index))
}

@Composable
private fun ShareFileCard(file: FileDescriptor) {
    val i18n = DI.current.get<I18nView>()
    val fileSize = "(" + (file.fileSize?.let { size -> formatSize(size) }
        ?: i18n.commonUnknown()) + ")"
    val isImage = file.mimeType?.match("image/*") == true
    val isVideo = file.mimeType?.match("video/*") == true
    val isAudio = file.mimeType?.match("audio/*") == true
    val (baseName, fileExtension) = splitFilename(file.fileName)
    Card(Modifier.height(MaterialTheme.messengerDpConstants.touchTarget)) {
        Row {
            Box(
                modifier = Modifier
                    .aspectRatio(1.0f)
                    .fillMaxSize()
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
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight()
                    .padding(
                        start = MaterialTheme.messengerDpConstants.small,
                        end = MaterialTheme.messengerDpConstants.middle
                    )
                    .widthIn(min = MaterialTheme.messengerDpConstants.touchTarget * 2)
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
                    fileSize,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
