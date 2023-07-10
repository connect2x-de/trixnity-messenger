package de.connect2x.trixnity.messenger.viewmodel.mock

import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.clientserverapi.model.media.ThumbnailResizingMethod
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.ThumbnailInfo
import net.folivo.trixnity.utils.ByteArrayFlow
import org.kodein.mock.Mocker

// TODO workaround for https://github.com/kosi-libs/MocKMP/issues/34
internal class MediaServiceMock(
    private val mocker: Mocker,
) : MediaService {
    public override suspend fun getEncryptedMedia(
        encryptedFile: EncryptedFile,
        progress: MutableStateFlow<FileTransferProgress?>?,
        saveToCache: Boolean,
    ): Result<ByteArrayFlow> = this.mocker.registerSuspend(
        this,
        "getEncryptedMedia(net.folivo.trixnity.core.model.events.m.room.EncryptedFile, kotlinx.coroutines.flow.MutableStateFlow, kotlin.Boolean)",
        encryptedFile, progress, saveToCache
    )

    public override suspend fun getMedia(
        uri: String,
        progress: MutableStateFlow<FileTransferProgress?>?,
        saveToCache: Boolean,
    ): Result<ByteArrayFlow> = this.mocker.registerSuspend(
        this,
        "getMedia(kotlin.String, kotlinx.coroutines.flow.MutableStateFlow, kotlin.Boolean)", uri,
        progress, saveToCache
    )

    public override suspend fun getThumbnail(
        mxcUri: String,
        width: Long,
        height: Long,
        method: ThumbnailResizingMethod,
        progress: MutableStateFlow<FileTransferProgress?>?,
        saveToCache: Boolean,
    ): Result<ByteArrayFlow> = this.mocker.registerSuspend(
        this,
        "getThumbnail(kotlin.String, kotlin.Long, kotlin.Long, net.folivo.trixnity.clientserverapi.model.media.ThumbnailResizingMethod, kotlinx.coroutines.flow.MutableStateFlow, kotlin.Boolean)",
        mxcUri, width, height, method, progress, saveToCache
    )

    public override suspend fun prepareUploadEncryptedMedia(content: ByteArrayFlow): EncryptedFile =
        this.mocker.registerSuspend(this, "prepareUploadEncryptedMedia(?)", content)

    public override suspend fun prepareUploadEncryptedThumbnail(
        content: ByteArrayFlow,
        contentType: ContentType?
    ): Pair<EncryptedFile, ThumbnailInfo>? =
        this.mocker.registerSuspend(
            this,
            "prepareUploadEncryptedThumbnail(?, io.ktor.http.ContentType)", content, contentType
        )

    public override suspend fun prepareUploadMedia(content: ByteArrayFlow, contentType: ContentType?):
            String = this.mocker.registerSuspend(
        this, "prepareUploadMedia(?, io.ktor.http.ContentType)",
        content, contentType
    )

    public override suspend fun prepareUploadThumbnail(
        content: ByteArrayFlow,
        contentType: ContentType?
    ): Pair<String, ThumbnailInfo>? = this.mocker.registerSuspend(
        this,
        "prepareUploadThumbnail(?, io.ktor.http.ContentType)", content, contentType
    )

    public override fun toString(): String = this.mocker.register(this, "toString()", default = {
        super.toString()
    })

    public override suspend fun uploadMedia(
        cacheUri: String,
        progress: MutableStateFlow<FileTransferProgress?>?,
        keepMediaInCache: Boolean,
    ): Result<String> = this.mocker.registerSuspend(
        this,
        "uploadMedia(kotlin.String, kotlinx.coroutines.flow.MutableStateFlow, kotlin.Boolean)",
        cacheUri, progress, keepMediaInCache
    )
}
