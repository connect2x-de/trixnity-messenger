package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.FilePickerType.IMAGE_FILE
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.files.LoadFileDialog
import de.connect2x.trixnity.messenger.compose.view.files.filterFilePickerOptionsByAvailability
import de.connect2x.trixnity.messenger.compose.view.files.rememberImageBitmapOrNull
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedDropdownMenu
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedDropdownMenuItem
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemCheckbox
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.room.ImagePackViewModel

private data class StickerRemoval(
    val pack: ImagePackViewModel.ImagePack,
    val sticker: ImagePackViewModel.ImagePack.Sticker,
)

@Composable
fun StickerPackPicker(model: ImagePackViewModel, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val i18n = DI.get<I18nView>()
    val packs = model.packs.collectAsState().value
    val canManage = model.canManageRoomPacks.collectAsState().value
    val isSaving = model.isSaving.collectAsState().value
    var showFilePicker by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<FileDescriptor?>(null) }
    var selectedPack by remember { mutableStateOf<ImagePackViewModel.ImagePack?>(null) }
    var packToRemove by remember { mutableStateOf<ImagePackViewModel.ImagePack?>(null) }
    var stickerToRemove by remember { mutableStateOf<StickerRemoval?>(null) }
    var showFavourites by remember { mutableStateOf(false) }
    val editingPacks = remember { mutableStateMapOf<String, Boolean>() }

    val focusManager = LocalFocusManager.current
    Column(
        modifier =
            modifier.fillMaxSize().pointerInput(focusManager) {
                detectTapGestures(onTap = { focusManager.clearFocus(force = true) })
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).padding(end = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tooltip({ Text(i18n.imagePackFavourites()) }) {
                ThemedIconButton(
                    style = MaterialTheme.components.commonIconButton,
                    onClick = { showFavourites = true },
                ) {
                    Icon(Icons.Default.Star, contentDescription = i18n.imagePackFavourites())
                }
            }
            if (canManage) {
                Tooltip({ Text(i18n.imagePackCreate()) }) {
                    ThemedIconButton(
                        style = MaterialTheme.components.commonIconButton,
                        onClick = {
                            selectedPack = null
                            showFilePicker = true
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = i18n.imagePackFavourites())
                    }
                }
            }
        }
        if (isSaving) {
            LoadingSpinner()
        }
        if (packs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(5.dp), contentAlignment = Alignment.Center) {
                Text(i18n.imagePacksEmpty())
            }
        } else {
            val listState = rememberLazyListState()
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val columns = ((maxWidth - 48.dp) / 60.dp).toInt().coerceAtLeast(1)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val (globalPacks, roomPacks) = packs.partition { it.isGloballyEnabled }
                    listOf("global" to globalPacks, "room" to roomPacks).forEach { (section, sectionPacks) ->
                        if (sectionPacks.isNotEmpty()) {
                            item(key = "$section-header") {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                                    if (section == "room" && globalPacks.isNotEmpty()) {
                                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                                    }
                                    Text(
                                        text =
                                            if (section == "global") i18n.imagePacksGlobal() else i18n.imagePacksRoom(),
                                        modifier = Modifier.semantics { heading() },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            sectionPacks.forEach { pack ->
                                val packKey = "${pack.roomId.full}:${pack.stateKey}"
                                val canManagePack = canManage && pack.belongsToCurrentRoom
                                val isEditing = canManagePack && editingPacks[packKey] == true
                                item(key = "$packKey-header", contentType = "pack-header") {
                                    StickerPackHeader(
                                        pack = pack,
                                        model = model,
                                        canManagePack = canManagePack,
                                        isEditing = isEditing,
                                        onToggleEditing = { editingPacks[packKey] = !isEditing },
                                        onRemovePack = { packToRemove = pack },
                                        onDismiss = onDismiss,
                                    )
                                }
                                val cellCount = pack.stickers.size + if (canManagePack) 1 else 0
                                items(
                                    count = (cellCount + columns - 1) / columns,
                                    key = { row -> "$packKey-row-$row" },
                                    contentType = { "sticker-row" },
                                ) { row ->
                                    StickerPackCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            for (index in row * columns until minOf((row + 1) * columns, cellCount)) {
                                                val sticker = pack.stickers.getOrNull(index)
                                                if (sticker != null) {
                                                    key(sticker.shortcode) {
                                                        StickerPackItem(
                                                            sticker = sticker,
                                                            onClick = {
                                                                model.send(
                                                                    pack.roomId,
                                                                    pack.stateKey,
                                                                    sticker.shortcode,
                                                                )
                                                                onDismiss()
                                                            },
                                                            onRemove =
                                                                if (isEditing) {
                                                                    { stickerToRemove = StickerRemoval(pack, sticker) }
                                                                } else null,
                                                        )
                                                    }
                                                } else {
                                                    Tooltip({ Text(i18n.imagePackAddSticker()) }) {
                                                        ThemedIconButton(
                                                            style = MaterialTheme.components.commonIconButton,
                                                            size = 56.dp,
                                                            onClick = {
                                                                selectedPack = pack
                                                                showFilePicker = true
                                                            },
                                                        ) {
                                                            Icon(Icons.Default.Add, i18n.imagePackAddSticker())
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(Modifier.align(Alignment.CenterEnd), listState, reverseLayout = false)
            }
        }
    }

    if (showFilePicker) {
        LoadFileDialog(
            filterFilePickerOptionsByAvailability(IMAGE_FILE),
            onFileSelect = {
                selectedFile = it
                showFilePicker = false
            },
            onCloseLoadFileDialog = { showFilePicker = false },
        )
    }

    selectedFile?.let { file ->
        StickerPackEditorDialog(
            model = model,
            pack = selectedPack,
            file = file,
            onDismiss = {
                selectedFile = null
                selectedPack = null
            },
        )
    }

    stickerToRemove?.let { removal ->
        ThemedModalDialog(onDismissRequest = { stickerToRemove = null }) {
            ModalDialogHeader { Text(i18n.imagePackRemoveSticker()) }
            ModalDialogContent { Text(i18n.imagePackRemoveStickerConfirmation(removal.sticker.body)) }
            ModalDialogFooter {
                Tooltip({ Text(i18n.commonCancel()) }) {
                    ThemedButton(
                        style = MaterialTheme.components.secondaryButton,
                        onClick = { stickerToRemove = null },
                    ) {
                        Text(i18n.commonCancel())
                    }
                }
                Tooltip({ Text(i18n.imagePackRemoveSticker()) }) {
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = {
                            model.removeImage(removal.pack.roomId, removal.pack.stateKey, removal.sticker.shortcode)
                            stickerToRemove = null
                        },
                    ) {
                        Text(i18n.commonRemove())
                    }
                }
            }
        }
    }

    packToRemove?.let { pack ->
        ThemedModalDialog(onDismissRequest = { packToRemove = null }) {
            ModalDialogHeader { Text(i18n.imagePackRemoveFromRoom()) }
            ModalDialogContent { Text(i18n.imagePackRemoveConfirmation(pack.displayName)) }
            ModalDialogFooter {
                Tooltip({ Text(i18n.commonCancel()) }) {
                    ThemedButton(style = MaterialTheme.components.secondaryButton, onClick = { packToRemove = null }) {
                        Text(i18n.commonCancel())
                    }
                }
                Tooltip({ Text(i18n.imagePackRemoveFromRoom()) }) {
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = {
                            model.removeFromRoom(pack.roomId, pack.stateKey)
                            packToRemove = null
                        },
                    ) {
                        Text(i18n.commonRemove())
                    }
                }
            }
        }
    }

    if (showFavourites) {
        StickerPackFavouritesDialog(
            packs = packs.filter { it.isGloballyEnabled },
            model = model,
            onDismiss = { showFavourites = false },
            onOpenRoom = { pack ->
                showFavourites = false
                onDismiss()
                model.openRoom(pack.roomId)
            },
        )
    }
}

@Composable
private fun StickerPackFavouritesDialog(
    packs: List<ImagePackViewModel.ImagePack>,
    model: ImagePackViewModel,
    onDismiss: () -> Unit,
    onOpenRoom: (ImagePackViewModel.ImagePack) -> Unit,
) {
    val i18n = DI.get<I18nView>()
    ThemedModalDialog(onDismissRequest = onDismiss) {
        ModalDialogHeader { Text(i18n.imagePackFavourites()) }
        ModalDialogContent {
            if (packs.isEmpty()) {
                Text(i18n.imagePackFavouritesEmpty())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    packs.forEach { pack ->
                        StickerPackCard {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                StickerPackInfo(
                                    pack,
                                    showRoom = true,
                                    modifier = Modifier.weight(1f),
                                    onOpenRoom = { onOpenRoom(pack) },
                                )
                                StickerPackFavouriteButton(pack, model)
                            }
                            StickerPackStrip(stickers = pack.stickers, previewLimit = 5)
                        }
                    }
                }
            }
        }
        ModalDialogFooter {
            Tooltip({ Text(i18n.commonClose()) }) {
                ThemedButton(style = MaterialTheme.components.primaryButton, onClick = onDismiss) {
                    Text(i18n.commonClose())
                }
            }
        }
    }
}

@Composable
private fun StickerPackHeader(
    pack: ImagePackViewModel.ImagePack,
    model: ImagePackViewModel,
    canManagePack: Boolean,
    isEditing: Boolean,
    onToggleEditing: () -> Unit,
    onRemovePack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    StickerPackCard(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StickerPackInfo(
                pack = pack,
                showRoom = pack.isGloballyEnabled && !pack.belongsToCurrentRoom,
                modifier = Modifier.weight(1f),
                onOpenRoom = {
                    onDismiss()
                    model.openRoom(pack.roomId)
                },
            )
            StickerPackFavouriteButton(pack, model)
            if (canManagePack) {
                Tooltip({ Text(if (isEditing) i18n.commonDone() else i18n.commonEdit()) }) {
                    ThemedIconButton(style = MaterialTheme.components.commonIconButton, onClick = onToggleEditing) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) i18n.commonDone() else i18n.commonEdit(),
                        )
                    }
                }
                if (isEditing) {
                    StickerRemovalMenu(label = i18n.imagePackRemoveFromRoom(), onRemove = onRemovePack)
                }
            }
        }
    }
}

@Composable
private fun StickerPackCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun StickerPackInfo(
    pack: ImagePackViewModel.ImagePack,
    showRoom: Boolean,
    modifier: Modifier = Modifier,
    onOpenRoom: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(pack.displayName, style = MaterialTheme.typography.titleSmall)
        if (showRoom) {
            val roomName = pack.roomName.collectAsState(initial = pack.roomId.full).value
            Text(
                i18n.imagePackFromRoom(roomName),
                modifier = Modifier.clickable(onClick = onOpenRoom).buttonPointerModifier(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        pack.attribution
            ?.takeIf { it.isNotBlank() }
            ?.let { attribution ->
                Text(
                    attribution,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
    }
}

@Composable
private fun StickerPackFavouriteButton(pack: ImagePackViewModel.ImagePack, model: ImagePackViewModel) {
    val i18n = DI.get<I18nView>()
    val label = if (pack.isGloballyEnabled) i18n.imagePackRemoveFromFavourites() else i18n.imagePackAddToFavourites()
    Tooltip({ Text(label) }) {
        ThemedIconButton(
            style = MaterialTheme.components.commonIconButton,
            onClick = { model.setGloballyEnabled(pack.roomId, pack.stateKey, !pack.isGloballyEnabled) },
        ) {
            Icon(if (pack.isGloballyEnabled) Icons.Default.Star else Icons.Default.StarOutline, label)
        }
    }
}

@Composable
private fun StickerPackStrip(
    stickers: List<ImagePackViewModel.ImagePack.Sticker>,
    modifier: Modifier = Modifier,
    previewLimit: Int = 5,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val hasMore = stickers.size > previewLimit
        val gap = 4.dp
        val count = minOf(stickers.size, previewLimit)
        val reservedWidth = if (hasMore) 40.dp + gap else 0.dp
        val stickerSize =
            minOf(
                48.dp,
                ((maxWidth - reservedWidth - gap * (count - 1).coerceAtLeast(0)) / count.coerceAtLeast(1))
                    .coerceAtLeast(0.dp),
            )
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            stickers.take(previewLimit).forEach { sticker ->
                key(sticker.shortcode) {
                    StickerPackItem(sticker = sticker, onClick = null, onRemove = null, size = stickerSize)
                }
            }
            if (hasMore) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        text = "+${stickers.size - previewLimit}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun StickerPackEditorDialog(
    model: ImagePackViewModel,
    pack: ImagePackViewModel.ImagePack?,
    file: FileDescriptor,
    onDismiss: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    var displayName by remember(pack) { mutableStateOf(pack?.displayName.orEmpty()) }
    var shortcode by remember(file) { mutableStateOf(file.fileName.substringBeforeLast('.')) }
    var body by remember(file) { mutableStateOf("") }
    val shortcodeAlreadyExists = pack?.stickers?.any { it.shortcode == shortcode.trim() } == true
    val shortcodeIsValid =
        !shortcodeAlreadyExists &&
            shortcode.matches(Regex("[a-zA-Z0-9-_]+")) &&
            shortcode.encodeToByteArray().size <= 100
    var unencryptedUploadAccepted by remember(file, pack?.roomId, pack?.stateKey) { mutableStateOf(false) }
    val canSave = displayName.isNotBlank() && shortcodeIsValid && unencryptedUploadAccepted
    val title =
        if (pack == null) {
            i18n.imagePackCreate()
        } else {
            i18n.imagePackAddSticker()
        }
    val saveLabel = i18n.imagePackUploadUnencrypted()

    val dialogStyle =
        MaterialTheme.components.modalDialog.let { style ->
            style.copy(maxWidth = 720.dp, container = style.container.copy(padding = PaddingValues(12.dp)))
        }
    ThemedModalDialog(onDismissRequest = onDismiss, modifier = Modifier.imePadding(), style = dialogStyle) {
        val focusManager = LocalFocusManager.current
        Column(
            Modifier.verticalScroll(rememberScrollState()).pointerInput(focusManager) {
                detectTapGestures(onTap = { focusManager.clearFocus(force = true) })
            }
        ) {
            ModalDialogHeader {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp).size(24.dp),
                        )
                    }
                    Column {
                        Text(title)
                        if (pack != null) {
                            Text(
                                pack.displayName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            Column(Modifier.padding(dialogStyle.container.contentPadding)) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = i18n.imagePackUnencryptedUploadWarning(), style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(32.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.fileName, style = MaterialTheme.typography.titleSmall)
                                file.mimeType?.let { mimeType ->
                                    Text(mimeType.toString(), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (pack == null) {
                        OutlinedTextField(
                            modifier = stickerEditorFieldModifier(),
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text(i18n.imagePackName()) },
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        modifier = stickerEditorFieldModifier(),
                        value = shortcode,
                        onValueChange = { shortcode = it },
                        label = { Text(i18n.imagePackShortcode()) },
                        isError = !shortcodeIsValid,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        modifier = stickerEditorFieldModifier(),
                        value = body,
                        onValueChange = { body = it },
                        label = { Text(i18n.imagePackDescription()) },
                        singleLine = true,
                    )
                    Text(
                        text =
                            if (shortcodeAlreadyExists) i18n.imagePackShortcodeAlreadyExists()
                            else i18n.imagePackShortcodeInvalid(),
                        color =
                            if (shortcodeIsValid) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            ThemedListItemCheckbox(
                headlineContent = { Text(i18n.imagePackUnencryptedUploadConsent()) },
                selected = unencryptedUploadAccepted,
                onChange = { unencryptedUploadAccepted = it },
            )
            ModalDialogFooter {
                Tooltip({ Text(i18n.commonCancel()) }) {
                    ThemedButton(style = MaterialTheme.components.secondaryButton, onClick = onDismiss) {
                        Text(i18n.commonCancel())
                    }
                }
                Tooltip({ Text(saveLabel) }) {
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        enabled = canSave,
                        onClick = {
                            if (canSave) {
                                model.addImage(
                                    pack?.roomId ?: model.roomId,
                                    pack?.stateKey,
                                    displayName,
                                    shortcode,
                                    body,
                                    file,
                                )
                                onDismiss()
                            }
                        },
                    ) {
                        Text(saveLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun stickerEditorFieldModifier(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(isFocused, imeBottom) { if (isFocused) requester.bringIntoView() }
    return Modifier.fillMaxWidth().bringIntoViewRequester(requester).onFocusChanged { isFocused = it.isFocused }
}

@Composable
private fun StickerPackItem(
    sticker: ImagePackViewModel.ImagePack.Sticker,
    onClick: (() -> Unit)?,
    onRemove: (() -> Unit)?,
    size: Dp = 56.dp,
) {
    val i18n = DI.get<I18nView>()
    val image = sticker.image.collectAsState().value
    val decodeSize = with(LocalDensity.current) { size.roundToPx().coerceIn(1, 256) }
    val bitmap = rememberImageBitmapOrNull(image, maxSize = IntSize(decodeSize, decodeSize))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier.size(size)
                    .then(
                        (onRemove ?: onClick)?.let { action ->
                            Modifier.clickable(onClick = action).buttonPointerModifier()
                        } ?: Modifier
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Tooltip({
                Text(
                    if (onRemove != null) i18n.imagePackRemoveSticker() else sticker.body.ifBlank { sticker.shortcode }
                )
            }) {
                if (bitmap != null) {
                    Image(
                        painter = BitmapPainter(bitmap),
                        contentDescription = sticker.body,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .padding(4.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = sticker.body,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(size * 0.45f),
                        )
                    }
                }
            }
        }
        if (onRemove != null) {
            Tooltip({ Text(i18n.imagePackRemoveSticker()) }) {
                ThemedIconButton(style = MaterialTheme.components.destructiveIconButton, onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = i18n.imagePackRemoveSticker())
                }
            }
        }
    }
}

@Composable
private fun StickerRemovalMenu(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
) {
    val i18n = DI.get<I18nView>()
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Tooltip({ Text(i18n.commonMore()) }) {
            ThemedIconButton(
                style = MaterialTheme.components.commonIconButton,
                size = size,
                onClick = { expanded = true },
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = i18n.commonMore(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ThemedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThemedDropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    expanded = false
                    onRemove()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            )
        }
    }
}

@Composable
fun StickerPackErrorDialog(model: ImagePackViewModel) {
    val error = model.error.collectAsState().value ?: return
    val i18n = DI.get<I18nView>()
    ThemedModalDialog(onDismissRequest = model::clearError) {
        ModalDialogHeader { Text(i18n.commonError()) }
        ModalDialogContent {
            Text(i18n.imagePackActionFailed())
            Spacer(Modifier.height(8.dp))
            Text(error)
        }
        ModalDialogFooter {
            Tooltip({ Text(i18n.commonOk()) }) {
                ThemedButton(style = MaterialTheme.components.primaryButton, onClick = model::clearError) {
                    Text(i18n.commonOk())
                }
            }
        }
    }
}
