package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback

interface FileMessageViewModelFactory {
    fun newFileMessageViewModel(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: StateFlow<Boolean>,
        sender: StateFlow<String>,
        invitation: Flow<String?>,
        content: FileMessageEventContent,
    ): FileMessageViewModel {
        return FileMessageViewModelImpl(
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
            content
        )
    }
}

interface FileMessageViewModel : FileBasedMessageViewModel {
    val url: String?
    val encryptedFile: EncryptedFile?
    val formattedSize: String
}

open class FileMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    override val showSender: StateFlow<Boolean>,
    override val sender: StateFlow<String>,
    override val invitation: Flow<String?>,
    private val content: FileMessageEventContent,
) : FileMessageViewModel, AbstractFileBasedMessageViewModel(viewModelContext),
    MatrixClientViewModelContext by viewModelContext {

    override val url: String? = content.file?.url ?: content.url
    override val encryptedFile: EncryptedFile? = content.file

    override fun getFileNameWithExtension() = content.fileName ?: content.bodyWithoutFallback

    override val formattedSize: String = content.info?.size?.let { " (${formatSize(it.toLong())})" } ?: ""
}