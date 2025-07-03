package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.IsFocused
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.EmojiSelector
import de.connect2x.messenger.compose.view.common.FilePickerType.ATTACHMENT_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.IMAGE_AND_VIDEO_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.PHOTO_CAPTURE
import de.connect2x.messenger.compose.view.common.FilePickerType.VIDEO_CAPTURE
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.customKeyNavigation
import de.connect2x.messenger.compose.view.files.EmptyFileListException
import de.connect2x.messenger.compose.view.files.LoadFileDialog
import de.connect2x.messenger.compose.view.files.NotPasteableException
import de.connect2x.messenger.compose.view.files.filterFilePickerOptionsByAvailability
import de.connect2x.messenger.compose.view.files.getClipboardFile
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.getOrNull
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.InputAreaStyle
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedIconToggleButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.messenger.compose.view.theme.components.ThemedUserAvatar
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
        val textField = inputAreaViewModel.textField.collectAsTextFieldValueState(focusRequester)

        ThemedSurface(
            style = MaterialTheme.components.inputAreaSurface,
        ) {
            Column(Modifier.fillMaxWidth()) {
                HorizontalDivider(Modifier.fillMaxWidth())
                if (isReplyTo) {
                    ReplyToArea(inputAreaViewModel)
                }
                if (emojisOpen.value) {
                    Box(Modifier.fillMaxWidth().height(120.dp)) {
                        EmojiSelector(Modifier.fillMaxSize().customKeyNavigation()) {
                            textField.value = textField.value.insert(it)
                            focusRequester.requestFocus()
                        }
                    }
                }

                UserSelector(inputAreaViewModel, focusRequester)
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            ThemedUserAvatar(userInfoElement.initials, avatar)
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
    focusRequester: FocusRequester,
    style: InputAreaStyle = MaterialTheme.components.inputArea,
) {
    val platformType = Platform.current
    val i18n = DI.get<I18nView>()
    val fileSystem = DI.getOrNull<FileSystem>() // TODO this does not work in Web
    val interactionSource = remember { MutableInteractionSource() }
    val showUploadError = remember { mutableStateOf<Throwable?>(null) }

    val maxAttachmentSize = DI.get<MatrixMessengerConfiguration>().maxMediaSizeInMemory

    if (Platform.current.isMobile.not()) {
        LaunchedEffect(Unit) {
            delay(500.milliseconds)
            focusRequester.requestFocus()
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .weight(1.0f, fill = true)
    ) {
        if (showUploadError.value != null) {
            ThemedModalDialog({ showUploadError.value = null }) {
                ModalDialogHeader {
                    Text(i18n.uploadFileErrorTitle())
                }
                ModalDialogContent {
                    Text(
                        when (showUploadError.value) {
                            is NotPasteableException -> i18n.uploadFileErrorNotPasteable()
                            is EmptyFileListException -> i18n.uploadFileErrorFileListEmpty()
                            else -> i18n.uploadFileErrorUnknown()
                        }
                    )
                }
                ModalDialogFooter {
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = { showUploadError.value = null },
                    ) {
                        Text(i18n.actionOk())
                    }
                }
            }
        }
        BasicTextField(
            cursorBrush = SolidColor(style.colors.cursorColor),
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
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
            interactionSource = interactionSource,
            maxLines = 6,
            textStyle = style.textStyle.copy(
                color = style.textColor(
                    enabled = true,
                    isError = false,
                    focused = IsFocused.current
                )
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            )
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
                        style = style.textStyle.copy(
                            color = style.placeholderColor(
                                enabled = true,
                                isError = false,
                                focused = IsFocused.current
                            )
                        ),
                    )
                },
                colors = style.colors,
                contentPadding = style.contentPadding,
            )
        }
    }
}

@Composable
fun EditButton(inputAreaViewModel: InputAreaViewModel) {
    val i18n = DI.get<I18nView>()
    Tooltip({ Text(i18n.inputAreaCancelEdit()) }) {
        ThemedIconButton(
            style = MaterialTheme.components.primaryIconButton,
            onClick = { inputAreaViewModel.cancelReplace() },
        ) {
            Icon(
                Icons.Default.EditOff,
                i18n.inputAreaCancelEdit(),
            )
        }
    }
}

@Composable
fun SendButton(inputAreaViewModel: InputAreaViewModel) {
    val i18n = DI.get<I18nView>()
    val enabled = inputAreaViewModel.isSendEnabled.collectAsState().value
    AnimatedVisibility(enabled, enter = fadeIn(), exit = fadeOut()) {
        Tooltip({ Text(i18n.inputAreaSend()) }) {
            ThemedIconButton(
                style = MaterialTheme.components.primaryIconButton,
                onClick = { inputAreaViewModel.sendMessage() },
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    i18n.inputAreaSend(),
                )
            }
        }
    }
}

@Composable
fun EmojiButton(emojisOpen: MutableState<Boolean>) {
    val i18n = DI.get<I18nView>()

    Tooltip({ Text(i18n.inputAreaEmojis()) }) {
        ThemedIconToggleButton(
            style = MaterialTheme.components.commonIconButton,
            checked = emojisOpen.value,
            onCheckedChange = { emojisOpen.value = emojisOpen.value.not() },
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
        Tooltip({ Text(i18n.inputAreaSelectAttachment()) }) {
            ThemedIconToggleButton(
                style = MaterialTheme.components.commonIconButton,
                checked = showAttachmentDialog,
                onCheckedChange = {
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
            ) {
                Icon(
                    MaterialTheme.messengerIcons.attachFile,
                    i18n.inputAreaSelectAttachment(),
                )
            }
        }
    }
}
