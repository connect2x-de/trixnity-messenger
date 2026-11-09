package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file

import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.core.model.events.m.room.FileInfo
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext

class FileTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    url: String?,
    encryptedFile: EncryptedFile?,
    info: FileInfo?,
    fileName: String,
) :
    FileBasedTimelineElementViewModel.File,
    FileBasedTimelineElementViewModelImpl(
        viewModelContext = viewModelContext,
        url = url,
        encryptedFile = encryptedFile,
        info = info,
        fileName = fileName,
    )
