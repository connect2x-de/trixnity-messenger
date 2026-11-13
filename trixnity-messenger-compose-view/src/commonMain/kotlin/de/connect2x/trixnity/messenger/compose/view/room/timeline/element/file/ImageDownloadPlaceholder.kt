package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.file

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.modifier.customClickable
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.FileBasedTimelineElementViewModel

@Composable
internal fun ImageDownloadPlaceholder(
    element: FileBasedTimelineElementViewModel.Image,
    showActionMenu: () -> Unit,
    download: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val loading =
        element.loadMediaProgress.collectAsState().value != null ||
            element.downloadMediaProgress.collectAsState().value != null ||
            element.thumbnailLoading.collectAsState().value
    val fallbackBlurhash =
        remember(element.name, element.size, element.width, element.height) {
            imagePlaceholderBlurhash(element.name, element.size, element.width, element.height)
        }
    val currentDownload by rememberUpdatedState(download)
    val isLoading by rememberUpdatedState(loading)
    Box(
        Modifier.padding(3.dp)
            .requiredHeightIn(50.dp, 300.dp)
            .timelineImageAspectRatio(element, defaultTimelineImageSize)
            .clip(RoundedCornerShape(8.dp))
            .customClickable(
                role = Role.Button,
                onClickLabel = i18n.downloadMessage(),
                onLongClickLabel = i18n.commonContextMenu(),
                onLongClick = showActionMenu,
                onClick = { if (!isLoading) currentDownload() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        ImageTimelineElementView(
            element,
            modifier = Modifier.matchParentSize(),
            showPlaceholder = false,
            showLoading = false,
            fallbackBlurhash = fallbackBlurhash,
        )
        Row(
            Modifier.clip(RoundedCornerShape(50))
                .background(MaterialTheme.messengerColors.metaDataPreviewBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (loading)
                ThemedProgressIndicator(
                    Modifier.size(24.dp),
                    MaterialTheme.components.extraSmallCircularProgressIndicator,
                )
            else
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.messengerColors.metaDataPreview,
                )
            Text(
                element.size?.takeIf { it.isNotBlank() } ?: i18n.downloadMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.messengerColors.metaDataPreview,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private val placeholderBlurhashes =
    listOf(
        "K9GuguMzS6VYNLX9RlSioy",
        "K9Ef~4Kib:K7TDoaS}kQr?",
        "K9EpTgTBrxKsnlrxbXrxnm",
        "K9GuRevpwJm^w3oares9XN",
        "K9FiAB\$vo{#jt2X9xUbuS6",
        "K9HU,wxvR*%MW=RjozR*WA",
    )

internal fun imagePlaceholderBlurhash(name: String, size: String?, width: Int?, height: Int?): String {
    val seed = listOf(name, size, width, height).hashCode() and Int.MAX_VALUE
    return placeholderBlurhashes[seed % placeholderBlurhashes.size]
}
