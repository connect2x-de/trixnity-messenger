package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.clientserverapi.model.media.ThumbnailResizingMethod
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.FileBasedInfo
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.ThumbnailInfo

private val log = KotlinLogging.logger { }

interface Thumbnails { // TODO this as part of the DI just adds complexity
    suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.File,
        thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>,
        maxMediaSizeInMemory: Long,
    ): ByteArray? =
        loadThumbnail(
            coroutineScope,
            matrixClient,
            content.info?.thumbnailFile,
            content.info?.thumbnailUrl,
            content.info?.thumbnailInfo,
            content.file,
            content.url,
            content.info,
            content.info?.size ?: Long.MAX_VALUE,
            thumbnailProgressFlow,
            maxMediaSizeInMemory,
        )

    suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.Image,
        thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>,
        maxMediaSizeInMemory: Long,
    ): ByteArray? =
        loadThumbnail(
            coroutineScope,
            matrixClient,
            content.info?.thumbnailFile,
            content.info?.thumbnailUrl,
            content.info?.thumbnailInfo,
            content.file,
            content.url,
            content.info,
            content.info?.size ?: Long.MAX_VALUE,
            thumbnailProgressFlow,
            maxMediaSizeInMemory,
        )

    suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.Video,
        thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>,
        maxMediaSizeInMemory: Long,
    ): ByteArray? =
        loadThumbnail(
            coroutineScope,
            matrixClient,
            content.info?.thumbnailFile,
            content.info?.thumbnailUrl,
            content.info?.thumbnailInfo,
            content.file,
            content.url,
            content.info,
            content.info?.size ?: Long.MAX_VALUE,
            thumbnailProgressFlow,
            maxMediaSizeInMemory,
        )

    suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        thumbnailFile: EncryptedFile?,
        thumbnailUrl: String?,
        thumbnailInfo: ThumbnailInfo?,
        file: EncryptedFile?,
        fileUrl: String?,
        fileInfo: FileBasedInfo?,
        sizeInBytes: Long,
        thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>,
        maxMediaSizeInMemory: Long,
    ): ByteArray?

    fun mapProgressToProgressElement(thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>): Flow<FileTransferProgressElement?>
}

class ThumbnailsImpl : Thumbnails {

    override suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        thumbnailFile: EncryptedFile?,
        thumbnailUrl: String?,
        thumbnailInfo: ThumbnailInfo?,
        file: EncryptedFile?,
        fileUrl: String?,
        fileInfo: FileBasedInfo?,
        sizeInBytes: Long,
        thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>,
        maxMediaSizeInMemory: Long,
    ): ByteArray? {
        log.debug { "thumbnail encrypted: ${thumbnailFile?.url}, unencrypted: $thumbnailUrl, encrypted file: ${file?.url}, unencrypted file: $fileUrl" }
        val thumbnail = (thumbnailFile?.let { // encrypted thumbnail
            matrixClient.media.getEncryptedMedia(
                thumbnailFile,
                thumbnailProgressFlow
            ).fold(
                onSuccess = {
                    it.toByteArray(
                        coroutineScope,
                        expectedSize = thumbnailInfo?.size,
                        maxSize = maxMediaSizeInMemory
                    )
                },
                onFailure = {
                    thumbnailProgressFlow.emit(null)
                    if (file != null && sizeInBytes <= maxMediaSizeInMemory) {
                        matrixClient.media.getEncryptedMedia(file, thumbnailProgressFlow).fold(
                            onSuccess = {
                                it.toByteArray(
                                    coroutineScope,
                                    expectedSize = fileInfo?.size,
                                    maxSize = maxMediaSizeInMemory
                                )
                            },
                            onFailure = {
                                log.error(it) { "Cannot load thumbnail for image '$thumbnailFile'." }
                                thumbnailProgressFlow.emit(null)
                                null
                            }
                        )
                    } else {
                        null
                    }
                }
            )
        } ?: thumbnailUrl?.let { // unencrypted thumbnail
            matrixClient.media.getThumbnail(
                thumbnailUrl,
                400L,
                300L,
                ThumbnailResizingMethod.SCALE,
                progress = thumbnailProgressFlow
            ).fold(
                onSuccess = {
                    it.toByteArray(
                        coroutineScope,
                        expectedSize = thumbnailInfo?.size,
                        maxSize = maxMediaSizeInMemory
                    )
                },
                onFailure = {  // fallback: real image
                    thumbnailProgressFlow.emit(null)
                    if (fileUrl != null && sizeInBytes <= maxMediaSizeInMemory) {
                        matrixClient.media.getMedia(fileUrl, thumbnailProgressFlow).fold(
                            onSuccess = {
                                it.toByteArray(
                                    coroutineScope,
                                    expectedSize = fileInfo?.size,
                                    maxSize = maxMediaSizeInMemory
                                )
                            },
                            onFailure = {
                                log.error(it) { "Cannot load thumbnail for image '$thumbnailUrl'." }
                                thumbnailProgressFlow.emit(null)
                                null
                            }
                        )
                    } else {
                        null
                    }
                }
            )
        } ?: file?.let { // encrypted file
            if (sizeInBytes <= maxMediaSizeInMemory) {
                matrixClient.media.getEncryptedMedia(file, thumbnailProgressFlow).fold(
                    onSuccess = {
                        it.toByteArray(
                            coroutineScope,
                            expectedSize = fileInfo?.size,
                            maxSize = maxMediaSizeInMemory
                        )
                    },
                    onFailure = {
                        log.error(it) { "Cannot load thumbnail for image '${file.url}'." }
                        thumbnailProgressFlow.emit(null)
                        null
                    }
                )
            } else {
                log.warn {
                    "there is no thumbnail for ${file.url}, but the file itself is considered too big to download as a thumbnail, so return `null`. " +
                            "Maybe the size of the file itself is undefined, so we assume it is too big to download."
                }
                null
            }
        } ?: fileUrl?.let { // unencrypted file
            // try to get server to generate thumbnail for us
            matrixClient.media.getThumbnail(
                fileUrl,
                400L,
                300L,
                ThumbnailResizingMethod.SCALE,
                progress = thumbnailProgressFlow
            ).fold(
                onSuccess = { it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory) },
                onFailure = {
                    thumbnailProgressFlow.emit(null)
                    // otherwise, see if the image itself is ok
                    if (sizeInBytes <= maxMediaSizeInMemory) {
                        matrixClient.media.getMedia(fileUrl, thumbnailProgressFlow).fold(
                            onSuccess = {
                                it.toByteArray(
                                    coroutineScope,
                                    expectedSize = fileInfo?.size,
                                    maxSize = maxMediaSizeInMemory
                                )
                            },
                            onFailure = {
                                log.error(it) { "Cannot load thumbnail for image '$fileUrl'." }
                                thumbnailProgressFlow.emit(null)
                                null
                            }
                        )
                    } else {
                        log.warn {
                            "there is no thumbnail for $fileUrl, but the file itself is considered too big to download as a thumbnail, so return `null`. " +
                                    "Maybe the size of the file itself is undefined, so we assume it is too big to download."
                        }
                        null
                    }
                })
        })
        return thumbnail
    }

    override fun mapProgressToProgressElement(thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>) =
        thumbnailProgressFlow.map {
            val total = it?.total
            if (total == null) {
                null
            } else {
                FileTransferProgressElement(
                    percent = if (total > 0) {
                        it.transferred / total.toFloat()
                    } else {
                        0f
                    },
                    formattedProgress = formatProgress(it)
                )
            }
        }
}
