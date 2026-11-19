package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.clientserverapi.client.DownloadLimitExceededException
import de.connect2x.trixnity.clientserverapi.model.media.FileTransferProgress
import de.connect2x.trixnity.clientserverapi.model.media.ThumbnailResizingMethod
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.core.model.events.m.room.FileBasedInfo
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.core.model.events.m.room.ThumbnailInfo
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface Thumbnails { // TODO this as part of the DI just adds complexity
    @Deprecated(
        "Use loadThumbnail with separate download size limit instead",
        ReplaceWith(
            "loadThumbnail(coroutineScope, matrixClient, content, onProgress, maxMediaSizeInMemory, maxDownloadSize)"
        ),
    )
    suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.File,
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
    ): ByteArray? =
        loadThumbnail(coroutineScope, matrixClient, content, onProgress, maxMediaSizeInMemory, maxMediaSizeInMemory)

    suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.File,
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
        maxDownloadSize: Long,
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
            onProgress,
            maxMediaSizeInMemory,
            maxDownloadSize,
        )

    @Deprecated(
        "Use loadThumbnail with separate download size limit instead",
        ReplaceWith(
            "loadThumbnail(coroutineScope, matrixClient, content, onProgress, maxMediaSizeInMemory, maxDownloadSize)"
        ),
    )
    suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.Image,
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
    ): ByteArray? =
        loadThumbnail(coroutineScope, matrixClient, content, onProgress, maxMediaSizeInMemory, maxMediaSizeInMemory)

    suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.Image,
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
        maxDownloadSize: Long,
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
            onProgress,
            maxMediaSizeInMemory,
            maxDownloadSize,
        )

    @Deprecated(
        "Use loadThumbnail with separate download size limit instead",
        ReplaceWith(
            "loadThumbnail(coroutineScope, matrixClient, content, onProgress, maxMediaSizeInMemory, maxDownloadSize)"
        ),
    )
    suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.Video,
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
    ): ByteArray? =
        loadThumbnail(coroutineScope, matrixClient, content, onProgress, maxMediaSizeInMemory, maxMediaSizeInMemory)

    suspend fun loadThumbnail(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        content: RoomMessageEventContent.FileBased.Video,
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
        maxDownloadSize: Long,
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
            onProgress,
            maxMediaSizeInMemory,
            maxDownloadSize,
        )

    @Deprecated(
        "Use loadThumbnail with separate download size limit instead",
        ReplaceWith(
            "loadThumbnail(" +
                "coroutineScope, " +
                "matrixClient, " +
                "thumbnailFile, " +
                "thumbnailUrl, " +
                "thumbnailInfo, " +
                "file, " +
                "fileUrl, " +
                "fileInfo, " +
                "sizeInBytes, " +
                "onProgress, " +
                "maxMediaSizeInMemory, " +
                "maxDownloadSize" +
                ")"
        ),
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
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
    ): ByteArray? =
        loadThumbnail(
            coroutineScope,
            matrixClient,
            thumbnailFile,
            thumbnailUrl,
            thumbnailInfo,
            file,
            fileUrl,
            fileInfo,
            sizeInBytes,
            onProgress,
            maxMediaSizeInMemory,
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
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
        maxDownloadSize: Long,
    ): ByteArray?

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
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
        maxDownloadSize: Long,
        width: Long,
        height: Long,
    ): ByteArray? =
        loadThumbnail(
            coroutineScope,
            matrixClient,
            thumbnailFile,
            thumbnailUrl,
            thumbnailInfo,
            file,
            fileUrl,
            fileInfo,
            sizeInBytes,
            onProgress,
            maxMediaSizeInMemory,
            maxDownloadSize,
        )

    fun mapProgressToProgressElement(
        thumbnailProgressFlow: Flow<FileTransferProgress?>
    ): Flow<FileTransferProgressElement?>
}

class ThumbnailsImpl : Thumbnails {
    companion object {
        private val log: Logger =
            Logger("de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.ThumbnailsImpl")
    }

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
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
        maxDownloadSize: Long,
    ): ByteArray? =
        loadThumbnail(
            coroutineScope,
            matrixClient,
            thumbnailFile,
            thumbnailUrl,
            thumbnailInfo,
            file,
            fileUrl,
            fileInfo,
            sizeInBytes,
            onProgress,
            maxMediaSizeInMemory,
            maxDownloadSize,
            400L,
            300L,
        )

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
        onProgress: (FileTransferProgress?) -> Unit,
        maxMediaSizeInMemory: Long,
        maxDownloadSize: Long,
        width: Long,
        height: Long,
    ): ByteArray? {
        log.debug {
            "thumbnail encrypted: ${thumbnailFile?.url}, unencrypted: $thumbnailUrl, encrypted file: ${file?.url}, unencrypted file: $fileUrl"
        }
        val thumbnail =
            (thumbnailFile?.let { // encrypted thumbnail
                matrixClient.media
                    .getEncryptedMedia(
                        thumbnailFile,
                        maxDownloadSize,
                        sizeInBytes,
                        onProgress = { onProgress(it.withFallbackTotal(thumbnailInfo?.size)) },
                    )
                    .fold(
                        onSuccess = {
                            it.toByteArray(
                                coroutineScope,
                                expectedSize = thumbnailInfo?.size,
                                maxSize = maxMediaSizeInMemory,
                            )
                        },
                        onFailure = {
                            onProgress(null)
                            if (file != null && sizeInBytes <= maxMediaSizeInMemory) {
                                matrixClient.media
                                    .getEncryptedMedia(
                                        file,
                                        maxDownloadSize,
                                        sizeInBytes,
                                        onProgress = { onProgress(it.withFallbackTotal(fileInfo?.size)) },
                                    )
                                    .fold(
                                        onSuccess = {
                                            it.toByteArray(
                                                coroutineScope,
                                                expectedSize = fileInfo?.size,
                                                maxSize = maxMediaSizeInMemory,
                                            )
                                        },
                                        onFailure = {
                                            if (it is DownloadLimitExceededException) {
                                                log.info {
                                                    "File skipped because it exceeds auto download limit '$thumbnailFile'"
                                                }
                                            } else {
                                                log.error(it) { "Cannot load thumbnail for image '$thumbnailFile'." }
                                            }
                                            onProgress(null)
                                            null
                                        },
                                    )
                            } else {
                                null
                            }
                        },
                    )
            }
                ?: thumbnailUrl?.let { // unencrypted thumbnail
                    matrixClient.media
                        .getThumbnail(
                            thumbnailUrl,
                            width,
                            height,
                            maxDownloadSize,
                            sizeInBytes,
                            ThumbnailResizingMethod.SCALE,
                            onProgress = onProgress,
                        )
                        .fold(
                            onSuccess = {
                                it.toByteArray(
                                    coroutineScope,
                                    expectedSize = thumbnailInfo?.size,
                                    maxSize = maxMediaSizeInMemory,
                                )
                            },
                            onFailure = { // fallback: real image
                                onProgress(null)
                                if (fileUrl != null && sizeInBytes <= maxMediaSizeInMemory) {
                                    matrixClient.media
                                        .getMedia(
                                            fileUrl,
                                            maxDownloadSize,
                                            sizeInBytes,
                                            onProgress = { onProgress(it.withFallbackTotal(fileInfo?.size)) },
                                        )
                                        .fold(
                                            onSuccess = {
                                                it.toByteArray(
                                                    coroutineScope,
                                                    expectedSize = fileInfo?.size,
                                                    maxSize = maxMediaSizeInMemory,
                                                )
                                            },
                                            onFailure = {
                                                if (it is DownloadLimitExceededException) {
                                                    log.info {
                                                        "File skipped because it exceeds auto download limit '$thumbnailUrl'"
                                                    }
                                                } else {
                                                    log.error(it) { "Cannot load thumbnail for image '$thumbnailUrl'." }
                                                }
                                                onProgress(null)
                                                null
                                            },
                                        )
                                } else {
                                    null
                                }
                            },
                        )
                }
                ?: file?.let { // encrypted file
                    if (sizeInBytes <= maxMediaSizeInMemory) {
                        matrixClient.media
                            .getEncryptedMedia(
                                file,
                                maxDownloadSize,
                                sizeInBytes,
                                onProgress = { onProgress(it.withFallbackTotal(fileInfo?.size)) },
                            )
                            .fold(
                                onSuccess = {
                                    it.toByteArray(
                                        coroutineScope,
                                        expectedSize = fileInfo?.size,
                                        maxSize = maxMediaSizeInMemory,
                                    )
                                },
                                onFailure = {
                                    if (it is DownloadLimitExceededException) {
                                        log.info { "File skipped because it exceeds auto download limit '${file.url}'" }
                                    } else {
                                        log.error(it) { "Cannot load thumbnail for image '${file.url}'." }
                                    }
                                    onProgress(null)
                                    null
                                },
                            )
                    } else {
                        log.warn {
                            "there is no thumbnail for ${file.url}, but the file itself is considered too big to download as a thumbnail, so return `null`. " +
                                "Maybe the size of the file itself is undefined, so we assume it is too big to download."
                        }
                        null
                    }
                }
                ?: fileUrl?.let { // unencrypted file
                    // try to get server to generate thumbnail for us
                    matrixClient.media
                        .getThumbnail(
                            fileUrl,
                            width,
                            height,
                            maxDownloadSize,
                            sizeInBytes,
                            ThumbnailResizingMethod.SCALE,
                            onProgress = onProgress,
                        )
                        .fold(
                            onSuccess = { it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory) },
                            onFailure = {
                                onProgress(null)
                                // otherwise, see if the image itself is ok
                                if (sizeInBytes <= maxMediaSizeInMemory) {
                                    matrixClient.media
                                        .getMedia(
                                            fileUrl,
                                            maxDownloadSize,
                                            sizeInBytes,
                                            onProgress = { onProgress(it.withFallbackTotal(fileInfo?.size)) },
                                        )
                                        .fold(
                                            onSuccess = {
                                                it.toByteArray(
                                                    coroutineScope,
                                                    expectedSize = fileInfo?.size,
                                                    maxSize = maxMediaSizeInMemory,
                                                )
                                            },
                                            onFailure = {
                                                if (it is DownloadLimitExceededException) {
                                                    log.info {
                                                        "File skipped because it exceeds auto download limit '$fileUrl'"
                                                    }
                                                } else {
                                                    log.error(it) { "Cannot load thumbnail for image '$fileUrl'." }
                                                }
                                                onProgress(null)
                                                null
                                            },
                                        )
                                } else {
                                    log.warn {
                                        "there is no thumbnail for $fileUrl, but the file itself is considered too big to download as a thumbnail, so return `null`. " +
                                            "Maybe the size of the file itself is undefined, so we assume it is too big to download."
                                    }
                                    null
                                }
                            },
                        )
                })
        return thumbnail
    }

    override fun mapProgressToProgressElement(
        thumbnailProgressFlow: Flow<FileTransferProgress?>
    ): Flow<FileTransferProgressElement?> {
        return thumbnailProgressFlow.map {
            val total = it?.total
            if (total == null) {
                null
            } else {
                FileTransferProgressElement(
                    percent =
                        if (total > 0) {
                            it.transferred / total.toFloat()
                        } else {
                            0f
                        },
                    formattedProgress = formatProgress(it),
                )
            }
        }
    }
}

private fun FileTransferProgress.withFallbackTotal(expectedSize: Long?): FileTransferProgress {
    if (total != null || expectedSize == null || expectedSize <= 0) return this
    return copy(total = expectedSize)
}
