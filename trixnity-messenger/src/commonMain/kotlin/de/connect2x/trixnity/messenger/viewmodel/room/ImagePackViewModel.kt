package de.connect2x.trixnity.messenger.viewmodel.room

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.flattenValues
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getAllState
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.getAccountData
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.ImagePackRoomsEventContent
import de.connect2x.trixnity.core.model.events.m.StickerEventContent
import de.connect2x.trixnity.core.model.events.m.room.ImageInfo
import de.connect2x.trixnity.core.model.events.m.room.ImagePackEventContent
import de.connect2x.trixnity.crypto.core.SecureRandom
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.openRoom
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.utils.nextString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.get

interface ImagePackViewModelFactory {
    fun create(viewModelContext: MatrixClientViewModelContext, roomId: RoomId): ImagePackViewModel?
}

class ImagePackViewModelFactoryImpl(private val configuration: MatrixMessengerConfiguration) :
    ImagePackViewModelFactory {
    override fun create(viewModelContext: MatrixClientViewModelContext, roomId: RoomId): ImagePackViewModel? =
        if (configuration.features.enableStickers) {
            ImagePackViewModelImpl(viewModelContext, roomId)
        } else {
            null
        }
}

interface ImagePackViewModel {
    val roomId: RoomId
    val packs: StateFlow<List<ImagePack>>
    val canSendStickers: StateFlow<Boolean>
    val canManageRoomPacks: StateFlow<Boolean>
    val isSaving: StateFlow<Boolean>
    val error: StateFlow<String?>

    fun openRoom(roomId: RoomId)

    fun send(roomId: RoomId, stateKey: String, shortcode: String)

    fun setGloballyEnabled(roomId: RoomId, stateKey: String, enabled: Boolean)

    fun addImage(
        roomId: RoomId,
        stateKey: String?,
        displayName: String,
        shortcode: String,
        body: String,
        file: FileDescriptor,
    )

    fun removeImage(roomId: RoomId, stateKey: String, shortcode: String)

    fun removeFromRoom(roomId: RoomId, stateKey: String)

    fun clearError()

    data class ImagePack(
        val roomId: RoomId,
        val roomName: Flow<String>,
        val stateKey: String,
        val displayName: String,
        val attribution: String?,
        val stickers: List<Sticker>,
        val belongsToCurrentRoom: Boolean,
        val isGloballyEnabled: Boolean,
    ) {
        data class Sticker(
            val shortcode: String,
            val body: String,
            val url: String,
            val info: ImageInfo,
            val image: StateFlow<ByteArray?> = MutableStateFlow(null),
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ImagePackViewModelImpl(viewModelContext: MatrixClientViewModelContext, override val roomId: RoomId) :
    MatrixClientViewModelContext by viewModelContext, ImagePackViewModel {
    private val thumbnails = get<Thumbnails>()
    private val configuration = get<MatrixMessengerConfiguration>()
    private val roomNames = get<RoomName>()
    private val i18n = get<I18n>()

    private val activeStickerSaves = MutableStateFlow(0)
    private val packStateUpdateMutex = Mutex()
    private val pendingPackWrites = mutableMapOf<String, PendingWrites<EventId, ImagePackEventContent>>()
    private val globalPackUpdateMutex = Mutex()
    private val pendingGlobalWrites = PendingWrites<ImagePackRoomsEventContent, ImagePackRoomsEventContent>()

    private val stickerImages = mutableMapOf<StickerImageKey, StateFlow<ByteArray?>>()
    private val accountData = matrixClient.user.getAccountData<ImagePackRoomsEventContent>()
    private val syncedAccountData =
        accountData
            .distinctUntilChanged()
            .runningFold(GlobalPackSnapshot(null, 0)) { previous, content ->
                GlobalPackSnapshot(content, previous.revision + 1)
            }
            .stateIn(coroutineScope, Eagerly, GlobalPackSnapshot(null, 0))

    private val roomPacks: Flow<List<LoadedPack>> =
        matrixClient.room
            .getAllState<ImagePackEventContent>(roomId)
            .map { it.orEmpty() }
            .flattenValues()
            .map { events ->
                events.mapNotNull { event ->
                    event.content
                        .takeIf { it.images.isNotEmpty() }
                        ?.let { content ->
                            LoadedPack(PackReference(roomId, event.stateKey), content, belongsToCurrentRoom = true)
                        }
                }
            }

    private val globalPacks: Flow<List<LoadedPack>> = accountData.flatMapLatest { content ->
        val references =
            content?.rooms.orEmpty().flatMap { (packRoomId, packs) -> packs.keys.map { PackReference(packRoomId, it) } }
        if (references.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(
                references.map { reference ->
                    matrixClient.room.getState<ImagePackEventContent>(reference.roomId, reference.stateKey).map { event
                        ->
                        event
                            ?.content
                            ?.takeIf { it.images.isNotEmpty() }
                            ?.let { content ->
                                LoadedPack(reference, content, belongsToCurrentRoom = reference.roomId == roomId)
                            }
                    }
                }
            ) { loadedPacks ->
                loadedPacks.filterNotNull()
            }
        }
    }

    override val packs: StateFlow<List<ImagePackViewModel.ImagePack>> =
        combine(globalPacks, roomPacks) { global, room ->
                val globallyEnabled = global.map { it.reference }.toSet()
                (global + room)
                    .distinctBy { it.reference }
                    .mapNotNull { loadedPack -> loadedPack.toStickerPack(globallyEnabled) }
                    .also { packs ->
                        val activeImages =
                            packs.flatMap { pack -> pack.stickers.map { StickerImageKey(it.url, it.info) } }.toSet()
                        stickerImages.keys.retainAll(activeImages)
                    }
            }
            .stateIn(coroutineScope, WhileSubscribed(), emptyList())

    override val canSendStickers: StateFlow<Boolean> =
        matrixClient.user.canSendEvent(roomId, StickerEventContent::class).stateIn(coroutineScope, Eagerly, false)

    override val canManageRoomPacks: StateFlow<Boolean> =
        matrixClient.user.canSendEvent(roomId, ImagePackEventContent::class).stateIn(coroutineScope, Eagerly, false)

    override val error = MutableStateFlow<String?>(null)
    override val isSaving: StateFlow<Boolean> =
        activeStickerSaves.map { it > 0 }.stateIn(coroutineScope, Eagerly, false)

    override fun openRoom(roomId: RoomId) {
        get<RouteNavigation>().updateNavigation { openRoom(userId, roomId) }
    }

    override fun send(roomId: RoomId, stateKey: String, shortcode: String) {
        if (!canSendStickers.value) {
            return
        }
        coroutineScope.launch {
            try {
                val sticker =
                    matrixClient.room
                        .getState<ImagePackEventContent>(roomId, stateKey)
                        .first()
                        ?.content
                        ?.images
                        ?.get(shortcode) ?: return@launch
                matrixClient.room.sendMessage(this@ImagePackViewModelImpl.roomId) {
                    content(
                        StickerEventContent(
                            url = sticker.url,
                            body = sticker.body ?: shortcode,
                            info = sticker.info ?: ImageInfo(),
                        )
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                log.error(exception) { "failed to send sticker $shortcode in room $roomId" }
                error.value = i18n.imagePackFailedToSend()
            }
        }
    }

    override fun setGloballyEnabled(roomId: RoomId, stateKey: String, enabled: Boolean) {
        coroutineScope.launch {
            try {
                updateGlobalPack(roomId, stateKey, enabled)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                log.error(exception) { "failed to update global image packs" }
                error.value = i18n.imagePackFailedToFavorize()
            }
        }
    }

    override fun addImage(
        roomId: RoomId,
        stateKey: String?,
        displayName: String,
        shortcode: String,
        body: String,
        file: FileDescriptor,
    ) {
        if (roomId != this.roomId || !canManageRoomPacks.value) {
            return
        }
        val normalizedShortcode = shortcode.trim()
        val shortcodeIsValid =
            normalizedShortcode.matches(shortCodeRegex) && normalizedShortcode.encodeToByteArray().size <= 100
        val errors = buildList {
            if (displayName.isBlank()) {
                add(i18n.imagePackInvalidImagePackDisplayName())
            }
            if (!shortcodeIsValid) {
                add(i18n.imagePackInvalidImagePackShortCode())
            }
            if (file.mimeType?.match("image/*") != true) {
                add(i18n.imagePackInvalidImagePackMustBeImage())
            }
        }

        if (errors.isNotEmpty()) {
            error.value = errors.joinToString(", ")
            return
        }

        activeStickerSaves.update { it + 1 }
        coroutineScope.launch {
            try {
                val cacheUri = matrixClient.media.prepareUploadMedia(file.content, file.mimeType)
                val mediaUrl = matrixClient.media.uploadMedia(cacheUri).getOrThrow()
                val targetStateKey = stateKey ?: createStateKey(displayName)
                val image =
                    ImagePackEventContent.ImagePackImage(
                        url = mediaUrl,
                        body = body.trim().ifEmpty { normalizedShortcode },
                        info = ImageInfo(mimeType = file.mimeType.toString(), size = file.fileSize),
                    )
                packStateUpdateMutex.withLock {
                    val pending = pendingPackWrites.getOrPut(targetStateKey) { PendingWrites() }
                    val oldContent = if (stateKey == null) null else currentPackContent(targetStateKey, pending)
                    if (normalizedShortcode in oldContent?.images.orEmpty()) {
                        error.value = i18n.imagePackShortcodeAlreadyExists()
                        return@withLock
                    }
                    val packMeta =
                        oldContent?.pack
                            ?: ImagePackEventContent.ImagePackMeta(
                                displayName = displayName.trim(),
                                usage = listOf(ImagePackEventContent.ImagePackMeta.Usage.Sticker),
                            )
                    val images = oldContent?.images.orEmpty() + (normalizedShortcode to image)
                    val content =
                        oldContent?.copy(images = images) ?: ImagePackEventContent(images = images, pack = packMeta)
                    val eventId = matrixClient.api.room.sendStateEvent(roomId, content, targetStateKey).getOrThrow()
                    pending.record(eventId, content)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                log.error(exception) { "failed to add sticker to image pack in room $roomId" }
                error.value = i18n.imagePackFailedToAddSticker()
            } finally {
                activeStickerSaves.update { it - 1 }
            }
        }
    }

    override fun removeFromRoom(roomId: RoomId, stateKey: String) {
        if (roomId != this.roomId || !canManageRoomPacks.value) {
            return
        }
        coroutineScope.launch {
            try {
                packStateUpdateMutex.withLock {
                    updateGlobalPack(roomId, stateKey, enabled = false)
                    val pending = pendingPackWrites.getOrPut(stateKey) { PendingWrites() }
                    currentPackContent(stateKey, pending)
                    val content = ImagePackEventContent(images = emptyMap())
                    val eventId = matrixClient.api.room.sendStateEvent(roomId, content, stateKey).getOrThrow()
                    pending.record(eventId, content)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                log.error(exception) { "failed to remove image pack $stateKey from room $roomId" }
                error.value = i18n.imagePackFailedToRemove()
            }
        }
    }

    override fun removeImage(roomId: RoomId, stateKey: String, shortcode: String) {
        if (roomId != this.roomId || !canManageRoomPacks.value) {
            return
        }
        coroutineScope.launch {
            try {
                packStateUpdateMutex.withLock {
                    val pending = pendingPackWrites.getOrPut(stateKey) { PendingWrites() }
                    val oldContent = currentPackContent(stateKey, pending) ?: return@withLock
                    if (shortcode !in oldContent.images) {
                        return@withLock
                    }
                    val newImages = oldContent.images - shortcode
                    if (newImages.isEmpty()) {
                        updateGlobalPack(roomId, stateKey, enabled = false)
                    }
                    val content = oldContent.copy(images = newImages)
                    val eventId = matrixClient.api.room.sendStateEvent(roomId, content, stateKey).getOrThrow()
                    pending.record(eventId, content)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                log.error(exception) { "failed to remove sticker $shortcode from image pack $stateKey" }
                error.value = i18n.imagePackFailedToRemoveSticker()
            }
        }
    }

    override fun clearError() {
        error.value = null
    }

    private fun createStateKey(displayName: String): String {
        val readablePart = displayName.lowercase().replace(Regex("[^a-z0-9-_]+"), "-").trim('-').take(40)
        val prefix = readablePart.ifEmpty { "stickers" }
        return "$prefix-${SecureRandom.nextString(8)}"
    }

    private suspend fun currentPackContent(
        stateKey: String,
        pending: PendingWrites<EventId, ImagePackEventContent>,
    ): ImagePackEventContent? {
        val synced = matrixClient.room.getState<ImagePackEventContent>(roomId, stateKey).first()
        return pending.resolve(synced?.id, synced?.content)
    }

    private suspend fun updateGlobalPack(roomId: RoomId, stateKey: String, enabled: Boolean) {
        globalPackUpdateMutex.withLock {
            val synced = syncedAccountData.filter { it.revision > 0 }.first()
            val oldContent =
                pendingGlobalWrites.resolve(synced.content, synced.content, synced.revision)
                    ?: ImagePackRoomsEventContent(emptyMap())
            val oldRoomPacks = oldContent.rooms[roomId].orEmpty()
            if ((stateKey in oldRoomPacks) == enabled) return@withLock
            val newRoomPacks =
                if (enabled) {
                    oldRoomPacks + (stateKey to Unit)
                } else {
                    oldRoomPacks - stateKey
                }
            val newRooms =
                if (newRoomPacks.isEmpty()) {
                    oldContent.rooms - roomId
                } else {
                    oldContent.rooms + (roomId to newRoomPacks)
                }
            val content = ImagePackRoomsEventContent(newRooms)
            matrixClient.api.user.setAccountData(content, matrixClient.userId).getOrThrow()
            pendingGlobalWrites.record(content, content)
        }
    }

    private fun LoadedPack.toStickerPack(globallyEnabled: Set<PackReference>): ImagePackViewModel.ImagePack? {
        val usage = content.pack?.usage
        val supportsStickers =
            usage.isNullOrEmpty() ||
                usage.any { it == ImagePackEventContent.ImagePackMeta.Usage.Sticker || it.value == "sticker" }
        if (!supportsStickers) {
            return null
        }
        return ImagePackViewModel.ImagePack(
            roomId = reference.roomId,
            roomName = roomNames.getRoomName(reference.roomId, matrixClient),
            stateKey = reference.stateKey,
            displayName = content.pack?.displayName ?: reference.stateKey.ifEmpty { reference.roomId.full },
            attribution = content.pack?.attribution,
            stickers =
                content.images.map { (shortcode, image) ->
                    ImagePackViewModel.ImagePack.Sticker(
                        shortcode = shortcode,
                        body = image.body ?: shortcode,
                        url = image.url,
                        info = image.info ?: ImageInfo(),
                        image =
                            stickerImages.getOrPut(StickerImageKey(image.url, image.info ?: ImageInfo())) {
                                loadStickerImage(image)
                            },
                    )
                },
            belongsToCurrentRoom = belongsToCurrentRoom,
            isGloballyEnabled = reference in globallyEnabled,
        )
    }

    private fun loadStickerImage(image: ImagePackEventContent.ImagePackImage): StateFlow<ByteArray?> {
        val info = image.info ?: ImageInfo()
        return flow {
                emit(
                    thumbnails.loadThumbnail(
                        coroutineScope = coroutineScope,
                        matrixClient = matrixClient,
                        thumbnailFile = info.thumbnailFile,
                        thumbnailUrl = info.thumbnailUrl,
                        thumbnailInfo = info.thumbnailInfo,
                        file = null,
                        fileUrl = image.url,
                        fileInfo = info,
                        sizeInBytes = info.size ?: Long.MAX_VALUE,
                        thumbnailProgressFlow = MutableStateFlow(null),
                        maxMediaSizeInMemory = minOf(configuration.maxMediaSizeInMemory, 4L * 1024 * 1024),
                        maxDownloadSize = minOf(configuration.downloadLimits.image, 4L * 1024 * 1024),
                        width = 256L,
                        height = 256L,
                    )
                )
            }
            .stateIn(coroutineScope, WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0), null)
    }

    private data class PackReference(val roomId: RoomId, val stateKey: String)

    private data class GlobalPackSnapshot(val content: ImagePackRoomsEventContent?, val revision: Long)

    private class PendingWrites<Version, Content> {
        private var baseVersion: Version? = null
        private var baseObservation: Long? = null
        private val writes = mutableListOf<Pair<Version, Content>>()

        fun resolve(version: Version?, content: Content?, observation: Long? = null): Content? {
            if (version != baseVersion || observation != baseObservation) {
                val confirmedIndex = writes.indexOfLast { it.first == version }
                if (confirmedIndex >= 0) {
                    writes.subList(0, confirmedIndex + 1).clear()
                } else {
                    writes.clear()
                }
            }
            baseVersion = version
            baseObservation = observation
            return writes.lastOrNull()?.second ?: content
        }

        fun record(version: Version, content: Content) {
            writes.add(version to content)
        }
    }

    private data class StickerImageKey(val url: String, val info: ImageInfo)

    private data class LoadedPack(
        val reference: PackReference,
        val content: ImagePackEventContent,
        val belongsToCurrentRoom: Boolean,
    )

    private companion object {
        private val shortCodeRegex by lazy { Regex("[a-zA-Z0-9-_]+") }
    }
}

class PreviewImagePackViewModel : ImagePackViewModel {
    override val roomId = RoomId("!preview:example.org")
    override val packs = MutableStateFlow<List<ImagePackViewModel.ImagePack>>(emptyList())
    override val canSendStickers = MutableStateFlow(true)
    override val canManageRoomPacks = MutableStateFlow(true)
    override val isSaving = MutableStateFlow(false)
    override val error = MutableStateFlow<String?>(null)

    override fun openRoom(roomId: RoomId) = Unit

    override fun send(roomId: RoomId, stateKey: String, shortcode: String) = Unit

    override fun setGloballyEnabled(roomId: RoomId, stateKey: String, enabled: Boolean) = Unit

    override fun addImage(
        roomId: RoomId,
        stateKey: String?,
        displayName: String,
        shortcode: String,
        body: String,
        file: FileDescriptor,
    ) = Unit

    override fun removeFromRoom(roomId: RoomId, stateKey: String) = Unit

    override fun removeImage(roomId: RoomId, stateKey: String, shortcode: String) = Unit

    override fun clearError() = Unit
}
