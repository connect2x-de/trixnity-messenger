package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.FileNameComputations
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import org.koin.core.component.get

interface AudioMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<String>,
        invitation: Flow<String?>,
        content: RoomMessageEventContent.AudioMessageEventContent,
        onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
    ): AudioMessageViewModel {
        return AudioMessageViewModelImpl(
            viewModelContext,
            formattedDate,
            showDateAbove,
            formattedTime,
            isByMe,
            showChatBubbleEdge,
            showBigGap,
            showSender,
            sender,
            invitation,
            content,
            onOpenModal,
        )
    }

    companion object : AudioMessageViewModelFactory
}

interface AudioMessageViewModel : FileBasedMessageViewModel {
    val url: String?
    val encryptedFile: EncryptedFile?
    fun openAudio()
}

open class AudioMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    showSender: Flow<Boolean>,
    sender: Flow<String>,
    invitation: Flow<String?>,
    private val content: RoomMessageEventContent.AudioMessageEventContent,
    private val onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
) : AudioMessageViewModel, AbstractFileBasedMessageViewModel(viewModelContext),
    MatrixClientViewModelContext by viewModelContext {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<String> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), "")
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)
    private val fileNameComputations = FileNameComputations(viewModelContext.get())

    override val url: String? = content.file?.url ?: content.url
    override val encryptedFile: EncryptedFile? = content.file

    override fun openAudio() {
        // TODO if you have audio working, replace with: 'url?.let { onOpenModal(OpenModalType.AUDIO, it, encryptedFile) }'
        openSaveFileDialog()
    }

    override fun getFileNameWithExtension() =
        fileNameComputations.getOrCreateFileName(
            content.bodyWithoutFallback,
            content.info?.mimeType,
            ContentType.Audio.Any
        )
}