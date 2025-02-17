package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.EmojiSelector
import de.connect2x.messenger.compose.view.common.ErrorDialog
import de.connect2x.messenger.compose.view.common.FilePickerType.ATTACHMENT_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.IMAGE_AND_VIDEO_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.PHOTO_CAPTURE
import de.connect2x.messenger.compose.view.common.FilePickerType.VIDEO_CAPTURE
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.files.EmptyFileListException
import de.connect2x.messenger.compose.view.files.LoadFileDialog
import de.connect2x.messenger.compose.view.files.NotPasteableException
import de.connect2x.messenger.compose.view.files.filterFilePickerOptionsByAvailability
import de.connect2x.messenger.compose.view.files.getClipboardFile
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.getOrNull
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.InputAreaViewModel
import kotlinx.coroutines.delay
import okio.FileSystem
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

private fun TextFieldValue.insert(insertion: String): TextFieldValue =
    TextFieldValue(
        this.text.substring(0, this.selection.start) + insertion + this.text.substring(this.selection.end),
        TextRange(
            this.selection.start + insertion.length - abs(this.selection.end - this.selection.start)
        )
    )

interface InputAreaView {
    @Composable
    fun create(inputAreaViewModel: InputAreaViewModel)
}

@Composable
fun InputArea(inputAreaViewModel: InputAreaViewModel) {
    DI.get<InputAreaView>().create(inputAreaViewModel)
}

class InputAreaViewImpl : InputAreaView {
    @Composable
    override fun create(inputAreaViewModel: InputAreaViewModel) {
        val i18n = DI.get<I18nView>()
        val isReplyTo = inputAreaViewModel.isReply.collectAsState().value
        val canSendMessages = inputAreaViewModel.isAllowedToSendMessages.collectAsState().value
        val isEdit = inputAreaViewModel.isReplace.collectAsState().value
        val emojisOpen = remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val textField = inputAreaViewModel.textField.collectAsTextFieldValueState()

        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
            Column(Modifier.fillMaxWidth()) {
                HorizontalDivider(Modifier.fillMaxWidth())
                if (isReplyTo) {
                    ReplyToArea(inputAreaViewModel)
                }
                if (emojisOpen.value) {
                    Box(Modifier.heightIn(max = 100.dp)) {
                        EmojiSelector({
                            textField.value = textField.value.insert(it)
                            focusRequester.requestFocus()
                        })
                    }
                }

                UserSelector(inputAreaViewModel, focusRequester)
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canSendMessages) {
                        EmojiButton(emojisOpen)

                        InputAreaTextField(inputAreaViewModel, textField, focusRequester)

                        if (isEdit) {
                            EditButton(inputAreaViewModel)
                        }
                        Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                            AttachmentButton(inputAreaViewModel)
                            SendButton(inputAreaViewModel)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth()) {
                            Text(
                                i18n.inputAreaCannotSendMessages(),
                                modifier = Modifier.padding(10.dp).align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSelector(inputAreaViewModel: InputAreaViewModel, focusRequester: FocusRequester) {
    val loading = inputAreaViewModel.listOfMentionsLoading.collectAsState().value
    val listOfMentions = inputAreaViewModel.listOfMentions.collectAsState().value
    val scrollState = rememberScrollState()

    if (listOfMentions != null || loading) {
        Box(
            Modifier
                .padding(vertical = 10.dp, horizontal = 20.dp)
                .heightIn(max = 150.dp)
        ) {
            if (loading) {
                LoadingSpinner(modifier = Modifier.heightIn(min = 150.dp))
            } else {
                Column(Modifier.verticalScroll(scrollState).fillMaxWidth()) {
                    listOfMentions.orEmpty().map { userInfoElement ->
                        val avatar = userInfoElement.image?.collectAsState(null)?.value
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    inputAreaViewModel.selectMention(userInfoElement.userId)
                                    focusRequester.requestFocus()
                                }
                                .buttonPointerModifier()
                                .padding(vertical = 5.dp)
                        ) {
                            Avatar(
                                avatar,
                                initials = userInfoElement.initials,
                                size = 28.dp
                            )
                            Spacer(Modifier.size(5.dp))
                            Text(userInfoElement.name, style = MaterialTheme.typography.bodyLarge)
                            Text(" (${userInfoElement.userId.full})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                VerticalScrollbar(
                    Modifier.align(Alignment.CenterEnd),
                    scrollState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.InputAreaTextField(
    inputAreaViewModel: InputAreaViewModel,
    textField: MutableState<TextFieldValue>,
    focusRequester: FocusRequester
) {
    val platformType = Platform.current
    val i18n = DI.get<I18nView>()
    val fileSystem = DI.getOrNull<FileSystem>() // TODO this does not work in Web
    val interactionSource = remember { MutableInteractionSource() }
    val showUploadError = remember { mutableStateOf<Throwable?>(null) }

    val maxAttachmentSize = DI.current.get<MatrixMessengerConfiguration>().maxMediaSizeInMemory

    Box(
        Modifier
            .fillMaxWidth()
            .padding(end = 8.dp, top = 8.dp, bottom = 8.dp)
            .weight(1.0f, fill = true)
    ) {
        if (showUploadError.value != null) {
            ErrorDialog(
                errorMessage = when (showUploadError.value) {
                    is NotPasteableException -> i18n.uploadFileErrorNotPasteable()
                    is EmptyFileListException -> i18n.uploadFileErrorFileListEmpty()
                    else -> i18n.uploadFileErrorUnknown()
                },
                dismissAction = { showUploadError.value = null }, title = i18n.uploadFileErrorTitle()
            )
        }
        BasicTextField(
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown) {
                        when {
                            (it.isShiftPressed && it.key == Key.Enter) -> {
                                textField.value = textField.value.insert("\n")
                                true
                            }

                            (it.key == Key.Enter && !platformType.isMobile) -> {
                                inputAreaViewModel.sendMessage()
                                true
                            }

                            ((it.isCtrlPressed || it.isMetaPressed) && it.key == Key.V) -> { // MacOS: Meta == Command?
                                val clipboardFile = fileSystem?.let { it1 -> getClipboardFile(it1, maxAttachmentSize) }
                                val fileContent = clipboardFile?.getOrNull()
                                if (fileContent != null) {
                                    inputAreaViewModel.onAttachmentFileSelect(fileContent)
                                    true
                                } else {
                                    showUploadError.value = clipboardFile?.exceptionOrNull()
                                    false
                                }
                            }

                            else -> false
                        }
                    } else {
                        false
                    }
                },
            value = textField.value,
            onValueChange = { textFieldValue ->
                textField.value = textFieldValue
            },
            maxLines = 6,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        ) { innerTextField ->
            @OptIn(ExperimentalMaterial3Api::class)
            OutlinedTextFieldDefaults.DecorationBox(
                value = textField.value.text,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = false,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        i18n.inputAreaPrompt(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                contentPadding = OutlinedTextFieldDefaults.contentPadding(
                    start = 7.dp, end = 7.dp, top = 7.dp, bottom = 7.dp,
                ),
            )
        }
    }
}

@Composable
fun EditButton(inputAreaViewModel: InputAreaViewModel) {
    val i18n = DI.get<I18nView>()
    val isMobile = Platform.current.isMobile
    Button(
        onClick = {
            inputAreaViewModel.cancelReplace()
        },
        modifier = Modifier // padding on desktop: 4.dp is 10.dp - 6.dp (border of text field)
            .padding(start = if (isMobile) 2.dp else 4.dp, end = if (isMobile) 8.dp else 10.dp)
            .size(if (isMobile) 40.dp else 34.dp)
            .buttonPointerModifier(),
        shape = CircleShape,
        contentPadding = PaddingValues(start = 2.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
    ) {
        Icon(
            imageVector = Icons.Default.EditOff,
            contentDescription = i18n.inputAreaCancelEdit(),
            modifier = if (isMobile) Modifier else Modifier.size(20.dp),
        )
    }
}

@Composable
fun SendButton(inputAreaViewModel: InputAreaViewModel) {
    val i18n = DI.get<I18nView>()
    val enabled = inputAreaViewModel.isSendEnabled.collectAsState().value
    val isMobile = Platform.current.isMobile
    AnimatedVisibility(enabled, enter = fadeIn(), exit = fadeOut()) {
        Button(
            onClick = {
                inputAreaViewModel.sendMessage()
            },
            modifier = Modifier // padding on desktop: 4.dp is 10.dp - 6.dp (border of text field)
                .padding(start = if (isMobile) 2.dp else 4.dp, end = if (isMobile) 8.dp else 10.dp)
                .size(if (isMobile) 36.dp else 34.dp)
                .buttonPointerModifier(enabled),
            shape = CircleShape,
            contentPadding = PaddingValues(start = 2.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
            enabled = enabled
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                i18n.inputAreaSend(),
                if (isMobile) Modifier else Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmojiButton(emojisOpen: MutableState<Boolean>) {
    val i18n = DI.get<I18nView>()
    val isMobile = Platform.current.isMobile
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
    ) {
        IconToggleButton(
            emojisOpen.value,
            { emojisOpen.value = emojisOpen.value.not() },
            Modifier.buttonPointerModifier().padding(horizontal = if (isMobile) 6.dp else 6.dp),
        ) {
            Icon(
                Icons.Default.Mood,
                i18n.inputAreaEmojis(),
            )
        }
    }
}

@Composable
fun AttachmentButton(inputAreaViewModel: InputAreaViewModel) {
    val i18n = DI.get<I18nView>()
    val isMobile = Platform.current.isMobile
    val showAttachmentDialog = inputAreaViewModel.showAttachmentSelectDialog.collectAsState().value
    val isSendEnabled = inputAreaViewModel.isSendEnabled.collectAsState().value
    if (showAttachmentDialog) LoadFileDialog(
        filterFilePickerOptionsByAvailability(
            ATTACHMENT_FILE,
            IMAGE_AND_VIDEO_FILE,
            PHOTO_CAPTURE,
            VIDEO_CAPTURE,
        ),
        inputAreaViewModel::onAttachmentFileSelect,
        inputAreaViewModel::closeAttachmentDialog,
    )
    AnimatedVisibility(isSendEnabled.not(), enter = fadeIn(), exit = fadeOut()) {
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides Dp.Unspecified
        ) {
            IconToggleButton(
                showAttachmentDialog,
                {
                    if (it) {
                        val hasShown = inputAreaViewModel.hasShownAttachmentSelectDialog.replayCache.getOrNull(0)
                        if (hasShown != null && hasShown) {
                            inputAreaViewModel.closeAttachmentDialog()
                        } else {
                            inputAreaViewModel.selectAttachment()
                        }
                    } else {
                        inputAreaViewModel.closeAttachmentDialog()
                    }
                },
                Modifier.buttonPointerModifier().padding(end = if (isMobile) 6.dp else 8.dp),
            ) {
                Icon(
                    MaterialTheme.messengerIcons.attachFile,
                    i18n.inputAreaSelectAttachment(),
                )
            }
        }
    }
}
