package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.clientserverapi.model.media.ThumbnailResizingMethod
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArray

private val log = KotlinLogging.logger { }

interface Thumbnails {
    suspend fun loadThumbnail(
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.Image,
        thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>,
    ): ByteArray? =
        loadThumbnail(
            matrixClient,
            content.info?.thumbnailFile,
            content.info?.thumbnailUrl,
            content.file,
            content.url,
            content.info?.size ?: Int.MAX_VALUE,
            thumbnailProgressFlow,
        )

    suspend fun loadThumbnail(
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.Video,
        thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>,
    ): ByteArray? =
        loadThumbnail(
            matrixClient,
            content.info?.thumbnailFile,
            content.info?.thumbnailUrl,
            content.file,
            content.url,
            content.info?.size ?: Int.MAX_VALUE,
            thumbnailProgressFlow,
        )

    suspend fun loadThumbnail(
        matrixClient: MatrixClient,
        thumbnailFile: EncryptedFile?,
        thumbnailUrl: String?,
        file: EncryptedFile?,
        url: String?,
        sizeInBytes: Int,
        thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>,
    ): ByteArray?

    fun mapProgressToProgressElement(thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>): Flow<FileTransferProgressElement?>
}

class ThumbnailsImpl : Thumbnails {
    private val maxThumbnailSize: Int = 1_000_000 // 1MB

    override suspend fun loadThumbnail(
        matrixClient: MatrixClient,
        thumbnailFile: EncryptedFile?,
        thumbnailUrl: String?,
        file: EncryptedFile?,
        url: String?,
        sizeInBytes: Int,
        thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>,
    ): ByteArray? {
        log.debug { "thumbnail encrypted: ${thumbnailFile?.url}, unencrypted: $thumbnailUrl, encrypted file: ${file?.url}, unencrypted file: $url" }
        return (thumbnailFile?.let { // encrypted thumbnail
            matrixClient.media.getEncryptedMedia(
                thumbnailFile,
                thumbnailProgressFlow
            ).fold(
                onSuccess = { it },
                onFailure = {
                    thumbnailProgressFlow.emit(null)
                    if (file != null && sizeInBytes < maxThumbnailSize) {
                        matrixClient.media.getEncryptedMedia(file, thumbnailProgressFlow).fold(
                            onSuccess = { it },
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
                onSuccess = { it },
                onFailure = {  // fallback: real image
                    thumbnailProgressFlow.emit(null)
                    if (url != null && sizeInBytes < maxThumbnailSize) {
                        matrixClient.media.getMedia(url, thumbnailProgressFlow).fold(
                            onSuccess = { it },
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
            if (sizeInBytes < maxThumbnailSize) {
                matrixClient.media.getEncryptedMedia(file, thumbnailProgressFlow).fold(
                    onSuccess = { it },
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
        } ?: url?.let { // unencrypted file
            // try to get server to generate thumbnail for us
            matrixClient.media.getThumbnail(
                url,
                400L,
                300L,
                ThumbnailResizingMethod.SCALE,
                progress = thumbnailProgressFlow
            ).fold(
                onSuccess = { it },
                onFailure = {
                    thumbnailProgressFlow.emit(null)
                    // otherwise, see if the image itself is ok
                    if (sizeInBytes < maxThumbnailSize) {
                        matrixClient.media.getMedia(url, thumbnailProgressFlow).fold(
                            onSuccess = { it },
                            onFailure = {
                                log.error(it) { "Cannot load thumbnail for image '$url'." }
                                thumbnailProgressFlow.emit(null)
                                null
                            }
                        )
                    } else {
                        log.warn {
                            "there is no thumbnail for $url, but the file itself is considered too big to download as a thumbnail, so return `null`. " +
                                    "Maybe the size of the file itself is undefined, so we assume it is too big to download."
                        }
                        null
                    }
                })
        })?.toByteArray()
    }

    override fun mapProgressToProgressElement(thumbnailProgressFlow: MutableStateFlow<FileTransferProgress?>) =
        thumbnailProgressFlow.map {
            if (it == null) {
                null
            } else {
                FileTransferProgressElement(
                    percent = if (it.total > 0) {
                        it.transferred / it.total.toFloat()
                    } else {
                        0f
                    },
                    formattedProgress = formatProgress(it)
                )
            }
        }
}