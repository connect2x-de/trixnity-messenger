package de.connect2x.messenger.compose.view.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.SINGLE_PANE_THRESHOLD
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.roomlist.room.RoomListElementContainer
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.sharing.ShareFilesViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

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

@Composable
private fun ShareFileCard(file: FileDescriptor) {
    val i18n = DI.current.get<I18nView>()
    val fileSize = "(" + (file.fileSize?.let { size -> formatSize(size) }
        ?: i18n.commonUnknown()) + ")"
    val isImage = file.mimeType?.match("image/*") == true
    val isVideo = file.mimeType?.match("video/*") == true
    val isAudio = file.mimeType?.match("audio/*") == true
    val baseName = file.fileName.substringBeforeLast('.')
    val fileExtension = "." + file.fileName.substringAfterLast('.')
    Card(Modifier.height(56.dp)) {
        Row {
            Box(
                modifier = Modifier
                    .aspectRatio(1.0f)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(
                    when {
                        isImage -> Icons.Default.Image
                        isVideo -> Icons.Default.Movie
                        isAudio -> Icons.Default.MusicNote
                        else -> Icons.Default.AttachFile
                    },
                    null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight()
                    .padding(start = 8.dp, end = 16.dp)
                    .widthIn(min = 112.dp)
            ) {
                Row {
                    Text(
                        baseName,
                        modifier = Modifier.widthIn(max = 180.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        fileExtension,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        softWrap = false
                    )
                }
                Text(
                    fileSize,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}


// Full screen on mobile, separate dialog on larger screens
@Composable
private fun AdaptiveDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isSinglePane = this@BoxWithConstraints.maxWidth < SINGLE_PANE_THRESHOLD.dp
        val maxContentHeight = min(1200.dp, maxHeight - (MaterialTheme.messengerDpConstants.large * 2))
        val maxContentWidth = 800.dp
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = !isSinglePane),
        ) {
            Box(
                if (isSinglePane) Modifier
                else Modifier.sizeIn(maxWidth = maxContentWidth, maxHeight = maxContentHeight)
            ) {
                Surface(
                    Modifier.fillMaxSize(),
                    if (isSinglePane) RectangleShape
                    else RoundedCornerShape(MaterialTheme.messengerDpConstants.small),
                    shadowElevation = 6.dp
                ) {
                    content()
                }
            }
        }
    }
}

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
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(viewModel.sharedFiles) { file ->
                        ShareFileCard(file)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxSize()) {
                    if (allRooms.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(Modifier.padding(horizontal = 20.dp)) {
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
