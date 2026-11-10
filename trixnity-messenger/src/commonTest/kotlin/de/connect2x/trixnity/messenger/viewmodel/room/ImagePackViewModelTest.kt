package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.room.message.MessageBuilder
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.client.UserApiClient
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.StateBaseEvent
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.m.ImagePackRoomsEventContent
import de.connect2x.trixnity.core.model.events.m.StickerEventContent
import de.connect2x.trixnity.core.model.events.m.room.ImageInfo
import de.connect2x.trixnity.core.model.events.m.room.ImagePackEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.BasicFileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.utils.toByteArrayFlow
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class ImagePackViewModelTest {
    private val configuration = MatrixMessengerConfiguration().apply { features.enableStickers = true }
    private val roomId = RoomId("!room:server")
    private val userId = UserId("@user:server")
    private val matrixClient = mock<MatrixClient>()
    private val roomService = mock<RoomService>()
    private val userService = mock<UserService>()
    private val mediaService = mock<MediaService>()
    private val thumbnails = mock<Thumbnails>()
    private val roomName = mock<RoomName>()
    private val api = mock<MatrixClientServerApiClient>()
    private val roomApi = mock<RoomApiClient>()
    private val userApi = mock<UserApiClient>()
    private val accountData = MutableStateFlow(ImagePackRoomsEventContent(emptyMap()))
    private val roomPacks = MutableStateFlow<Map<String, Flow<StateBaseEvent<ImagePackEventContent>?>>>(emptyMap())

    init {
        resetMocks(matrixClient, roomService, userService, mediaService, thumbnails, roomName, api, roomApi, userApi)
        every { matrixClient.di } returns
            koinApplication {
                    modules(
                        module {
                            single { roomService }
                            single { userService }
                            single { mediaService }
                            single { thumbnails }
                            single { configuration }
                        }
                    )
                }
                .koin
        every { matrixClient.userId } returns userId
        every { matrixClient.api } returns api
        every { api.room } returns roomApi
        every { api.user } returns userApi
        every { userService.getAccountData(ImagePackRoomsEventContent::class) } returns accountData
        every { roomService.getAllState(roomId, ImagePackEventContent::class) } returns roomPacks
        every { userService.canSendEvent(roomId, any<KClass<out RoomEventContent>>()) } returns flowOf(true)
        every { roomName.getRoomName(any<RoomId>(), matrixClient, any()) } returns flowOf("Cats room")
    }

    @Test
    fun `factory returns null when stickers are disabled`() = runTest {
        configuration.features.enableStickers = false

        createViewModel() shouldBe null
    }

    @Test
    fun `loads sticker packs from room state`() = runTest {
        roomPacks.value = mapOf("cats" to flowOf<StateBaseEvent<ImagePackEventContent>?>(packEvent("cats")))

        val pack = cut().packs.first { it.isNotEmpty() }.single()

        pack.displayName shouldBe "Cats"
        pack.roomName.first() shouldBe "Cats room"
        pack.belongsToCurrentRoom shouldBe true
        pack.stickers.map { it.shortcode } shouldContainExactly listOf("wave")
    }

    @Test
    fun `large packs only load subscribed previews and release them when hidden`() = runTest {
        val event = packEvent("large")
        roomPacks.value =
            mapOf(
                "large" to
                    flowOf(
                        event.copy(
                            content =
                                event.content.copy(
                                    images =
                                        (0 until 1000).associate { index ->
                                            "sticker-$index" to
                                                ImagePackEventContent.ImagePackImage(url = "mxc://server/$index")
                                        }
                                )
                        )
                    )
            )
        var requests = 0
        val bytes = byteArrayOf(1, 2, 3)
        everySuspend {
            thumbnails.loadThumbnail(
                coroutineScope = any(),
                matrixClient = matrixClient,
                thumbnailFile = any(),
                thumbnailUrl = any(),
                thumbnailInfo = any(),
                file = any(),
                fileUrl = any(),
                fileInfo = any(),
                sizeInBytes = any(),
                thumbnailProgressFlow = any(),
                maxMediaSizeInMemory = 4L * 1024 * 1024,
                maxDownloadSize = 4L * 1024 * 1024,
                width = 256L,
                height = 256L,
            )
        } calls
            {
                requests++
                bytes
            }
        val cut = cut()
        backgroundScope.launch { cut.packs.collect() }
        testScheduler.runCurrent()
        val stickers = cut.packs.value.single().stickers
        requests shouldBe 0
        val collector = backgroundScope.launch { stickers.first().image.collect() }
        testScheduler.runCurrent()
        requests shouldBe 1
        stickers.first().image.value shouldBe bytes
        stickers.last().image.value shouldBe null
        collector.cancel()
        testScheduler.runCurrent()
        stickers.first().image.value shouldBe null
    }

    @Test
    fun `preserves sticker image state when pack metadata or other stickers change`() = runTest {
        val event = MutableStateFlow(packEvent("cats"))
        roomPacks.value = mapOf("cats" to event)
        val cut = cut()
        backgroundScope.launch { cut.packs.collect() }
        testScheduler.runCurrent()
        val original = cut.packs.value.single().stickers.single().image

        event.value =
            event.value.copy(
                content =
                    event.value.content.copy(
                        pack = ImagePackEventContent.ImagePackMeta(displayName = "Renamed cats"),
                        images =
                            event.value.content.images +
                                ("sleep" to ImagePackEventContent.ImagePackImage(url = "mxc://server/sleep")),
                    )
            )
        testScheduler.runCurrent()

        val pack = cut.packs.value.single()
        pack.displayName shouldBe "Renamed cats"
        pack.stickers.size shouldBe 2
        (pack.stickers.first { it.shortcode == "wave" }.image === original) shouldBe true
    }

    @Test
    fun `replaces sticker image state when image source changes`() = runTest {
        val event = MutableStateFlow(packEvent("cats"))
        roomPacks.value = mapOf("cats" to event)
        val cut = cut()
        backgroundScope.launch { cut.packs.collect() }
        testScheduler.runCurrent()
        val original = cut.packs.value.single().stickers.single().image

        val image = event.value.content.images.getValue("wave")
        event.value =
            event.value.copy(
                content = event.value.content.copy(images = mapOf("wave" to image.copy(url = "mxc://server/new-wave")))
            )
        testScheduler.runCurrent()

        (cut.packs.value.single().stickers.single().image === original) shouldBe false
    }

    @Test
    fun `loads packs with stable sticker usage value`() = runTest {
        val usage = ImagePackEventContent.ImagePackMeta.Usage.Unknown("sticker")
        roomPacks.value = mapOf("cats" to flowOf<StateBaseEvent<ImagePackEventContent>?>(packEvent("cats", usage)))

        cut().packs.first { it.isNotEmpty() }.single().displayName shouldBe "Cats"
    }

    @Test
    fun `uses image pack power level for room management`() = runTest {
        every { userService.canSendEvent(roomId, ImagePackEventContent::class) } returns flowOf(false)

        val cut = cut()
        testScheduler.runCurrent()

        cut.canManageRoomPacks.value shouldBe false
    }

    @Test
    fun `sends sticker from another room pack to current room`() = runTest {
        var sentContent: StickerEventContent? = null
        everySuspend { roomService.sendMessage(roomId, any(), any()) } calls
            {
                val builder = it.arg<suspend MessageBuilder.() -> Unit>(2)
                sentContent =
                    MessageBuilder(roomId, roomService, mediaService, userId).build(builder) as StickerEventContent
                "transaction"
            }
        val packRoomId = RoomId("!pack:server")
        every { roomService.getState(packRoomId, ImagePackEventContent::class, "cats") } returns
            flowOf(packEvent("cats").copy(roomId = packRoomId))

        val cut = cut()
        testScheduler.runCurrent()
        cut.send(packRoomId, "cats", "wave")
        testScheduler.runCurrent()

        sentContent shouldBe StickerEventContent("mxc://server/wave", "Waving cat", ImageInfo(width = 64, height = 64))
    }

    @Test
    fun `adds room pack to global account data`() = runTest {
        everySuspend { userApi.setAccountData(any<ImagePackRoomsEventContent>(), userId, any()) } returns
            Result.success(Unit)
        val pack =
            ImagePackViewModel.ImagePack(
                roomId = roomId,
                roomName = flowOf(roomId.full),
                stateKey = "cats",
                displayName = "Cats",
                attribution = null,
                stickers = emptyList(),
                belongsToCurrentRoom = true,
                isGloballyEnabled = false,
            )

        cut().setGloballyEnabled(pack.roomId, pack.stateKey, true)
        testScheduler.runCurrent()

        verifySuspend {
            userApi.setAccountData(ImagePackRoomsEventContent(mapOf(roomId to mapOf("cats" to Unit))), userId, any())
        }
    }

    @Test
    fun `preserves overlapping global pack updates`() = runTest {
        val writtenContent = mutableListOf<ImagePackRoomsEventContent>()
        everySuspend { userApi.setAccountData(any<ImagePackRoomsEventContent>(), userId, any()) } calls
            {
                writtenContent.add(it.arg(0))
                Result.success(Unit)
            }
        val cats =
            ImagePackViewModel.ImagePack(
                roomId = roomId,
                roomName = flowOf(roomId.full),
                stateKey = "cats",
                displayName = "Cats",
                attribution = null,
                stickers = emptyList(),
                belongsToCurrentRoom = true,
                isGloballyEnabled = false,
            )
        val dogs = cats.copy(stateKey = "dogs", displayName = "Dogs")
        val cut = cut()
        testScheduler.runCurrent()

        cut.setGloballyEnabled(cats.roomId, cats.stateKey, true)
        cut.setGloballyEnabled(dogs.roomId, dogs.stateKey, true)
        testScheduler.runCurrent()

        writtenContent.last().rooms[roomId]?.keys shouldBe setOf("cats", "dogs")
    }

    @Test
    fun `creates sticker pack with uploaded image`() = runTest {
        var sentContent: ImagePackEventContent? = null
        everySuspend { mediaService.prepareUploadMedia(any(), ContentType.Image.PNG) } returns "cache://sticker"
        everySuspend { mediaService.uploadMedia("cache://sticker") } returns Result.success("mxc://server/sticker")
        everySuspend { roomApi.sendStateEvent(roomId, any<ImagePackEventContent>(), any()) } calls
            {
                sentContent = it.arg(1)
                Result.success(EventId("event"))
            }
        val file =
            BasicFileDescriptor(
                fileName = "wave.png",
                fileSize = 4,
                mimeType = ContentType.Image.PNG,
                content = byteArrayOf(1, 2, 3, 4).toByteArrayFlow(),
            )
        val cut = cut()
        testScheduler.runCurrent()

        cut.addImage(roomId, null, "Cats", "wave", "Waving cat", file)
        testScheduler.runCurrent()

        sentContent?.images?.get("wave")?.url shouldBe "mxc://server/sticker"
        sentContent?.pack?.displayName shouldBe "Cats"
    }

    @Test
    fun `uploads stickers concurrently and preserves both pack updates`() = runTest {
        val bothUploadsStarted = CompletableDeferred<Unit>()
        val releaseUploads = CompletableDeferred<Unit>()
        val sentContents = mutableListOf<ImagePackEventContent>()
        var preparedUploadCount = 0
        everySuspend { mediaService.prepareUploadMedia(any(), ContentType.Image.PNG) } calls
            {
                preparedUploadCount += 1
                "cache://sticker-$preparedUploadCount"
            }
        everySuspend { mediaService.uploadMedia(any()) } calls
            {
                val cacheUri = it.arg<String>(0)
                if (cacheUri == "cache://sticker-2") {
                    bothUploadsStarted.complete(Unit)
                }
                bothUploadsStarted.await()
                releaseUploads.await()
                Result.success(cacheUri.replace("cache://", "mxc://server/"))
            }
        every { roomService.getState(roomId, ImagePackEventContent::class, "cats") } returns flowOf(packEvent("cats"))
        everySuspend { roomApi.sendStateEvent(roomId, any<ImagePackEventContent>(), "cats") } calls
            {
                sentContents.add(it.arg(1))
                Result.success(EventId("event-${sentContents.size}"))
            }
        val pack =
            ImagePackViewModel.ImagePack(
                roomId = roomId,
                roomName = flowOf(roomId.full),
                stateKey = "cats",
                displayName = "Cats",
                attribution = null,
                stickers = emptyList(),
                belongsToCurrentRoom = true,
                isGloballyEnabled = false,
            )
        val firstFile = stickerFile("jump.png")
        val secondFile = stickerFile("sleep.png")
        val cut = cut()
        testScheduler.runCurrent()

        cut.addImage(pack.roomId, pack.stateKey, pack.displayName, "jump", "Jumping cat", firstFile)
        cut.addImage(pack.roomId, pack.stateKey, pack.displayName, "sleep", "Sleeping cat", secondFile)
        testScheduler.runCurrent()

        bothUploadsStarted.isCompleted shouldBe true
        cut.isSaving.value shouldBe true

        releaseUploads.complete(Unit)
        testScheduler.runCurrent()

        cut.isSaving.value shouldBe false
        sentContents.last().images.keys shouldBe setOf("wave", "jump", "sleep")
    }

    @Test
    fun `rejects existing shortcode including surrounding whitespace`() = runTest {
        val sentContents = mutableListOf<ImagePackEventContent>()
        everySuspend { mediaService.prepareUploadMedia(any(), ContentType.Image.PNG) } returns "cache://sticker"
        everySuspend { mediaService.uploadMedia(any()) } returns Result.success("mxc://server/new")
        every { roomService.getState(roomId, ImagePackEventContent::class, "cats") } returns flowOf(packEvent("cats"))
        everySuspend { roomApi.sendStateEvent(roomId, any<ImagePackEventContent>(), "cats") } calls
            {
                sentContents.add(it.arg(1))
                Result.success(EventId("event"))
            }
        val cut = cut()
        testScheduler.runCurrent()

        cut.addImage(roomId, "cats", "Cats", " wave ", "New cat", stickerFile("wave.png"))
        testScheduler.runCurrent()

        sentContents shouldBe emptyList()
        cut.error.value.isNullOrBlank() shouldBe false
        cut.isSaving.value shouldBe false
    }

    @Test
    fun `overlapping additions with same shortcode preserve the first sticker`() = runTest {
        val releaseUploads = CompletableDeferred<Unit>()
        val sentContents = mutableListOf<ImagePackEventContent>()
        everySuspend { mediaService.prepareUploadMedia(any(), ContentType.Image.PNG) } returns "cache://sticker"
        everySuspend { mediaService.uploadMedia(any()) } calls
            {
                releaseUploads.await()
                Result.success("mxc://server/new")
            }
        every { roomService.getState(roomId, ImagePackEventContent::class, "cats") } returns flowOf(packEvent("cats"))
        everySuspend { roomApi.sendStateEvent(roomId, any<ImagePackEventContent>(), "cats") } calls
            {
                sentContents.add(it.arg(1))
                Result.success(EventId("event"))
            }
        val cut = cut()
        testScheduler.runCurrent()
        cut.addImage(roomId, "cats", "Cats", "jump", "First cat", stickerFile("jump.png"))
        cut.addImage(roomId, "cats", "Cats", " jump ", "Second cat", stickerFile("jump.png"))
        testScheduler.runCurrent()
        releaseUploads.complete(Unit)
        testScheduler.runCurrent()

        sentContents.size shouldBe 1
        sentContents.single().images.keys shouldBe setOf("wave", "jump")
        sentContents.single().images["jump"]?.body shouldBe "First cat"
        cut.error.value.isNullOrBlank() shouldBe false
        cut.isSaving.value shouldBe false
    }

    @Test
    fun `removal preserves another users synced additions and metadata after a local write`() = runTest {
        val synced = MutableStateFlow(packEvent("cats"))
        val writes = mutableListOf<ImagePackEventContent>()
        mockPackWrites(synced, writes)
        val cut = cut()
        testScheduler.runCurrent()
        cut.addImage(roomId, "cats", "Cats", "jump", "Jump", stickerFile("jump.png"))
        testScheduler.runCurrent()
        synced.value =
            synced.value.copy(
                id = EventId("remote"),
                content =
                    writes
                        .last()
                        .copy(
                            images =
                                writes.last().images +
                                    ("remote" to ImagePackEventContent.ImagePackImage("mxc://server/remote")),
                            pack =
                                ImagePackEventContent.ImagePackMeta(
                                    displayName = "Renamed remotely",
                                    attribution = "Remote author",
                                ),
                        ),
            )
        cut.removeImage(roomId, "cats", "wave")
        testScheduler.runCurrent()

        writes.last().images.keys shouldBe setOf("jump", "remote")
        writes.last().pack shouldBe synced.value.content.pack
    }

    @Test
    fun `addition does not resurrect another users synced removal after a local write`() = runTest {
        val synced = MutableStateFlow(packEvent("cats"))
        val writes = mutableListOf<ImagePackEventContent>()
        mockPackWrites(synced, writes)
        val cut = cut()
        testScheduler.runCurrent()
        cut.addImage(roomId, "cats", "Cats", "jump", "Jump", stickerFile("jump.png"))
        testScheduler.runCurrent()
        synced.value =
            synced.value.copy(
                id = EventId("remote"),
                content = writes.last().copy(images = writes.last().images - "wave"),
            )
        cut.addImage(roomId, "cats", "Cats", "sleep", "Sleep", stickerFile("sleep.png"))
        testScheduler.runCurrent()

        writes.last().images.keys shouldBe setOf("jump", "sleep")
    }

    @Test
    fun `intermediate pack sync echo does not discard later pending writes`() = runTest {
        val synced = MutableStateFlow(packEvent("cats"))
        val writes = mutableListOf<ImagePackEventContent>()
        mockPackWrites(synced, writes)
        val cut = cut()
        testScheduler.runCurrent()
        cut.addImage(roomId, "cats", "Cats", "jump", "Jump", stickerFile("jump.png"))
        testScheduler.runCurrent()
        cut.addImage(roomId, "cats", "Cats", "sleep", "Sleep", stickerFile("sleep.png"))
        testScheduler.runCurrent()
        synced.value = synced.value.copy(id = EventId("local-1"), content = writes.first())
        cut.removeImage(roomId, "cats", "wave")
        testScheduler.runCurrent()

        writes.last().images.keys shouldBe setOf("jump", "sleep")
    }

    @Test
    fun `global updates preserve synced changes from another device`() = runTest {
        val writes = mutableListOf<ImagePackRoomsEventContent>()
        everySuspend { userApi.setAccountData(any<ImagePackRoomsEventContent>(), userId, any()) } calls
            {
                writes.add(it.arg(0))
                Result.success(Unit)
            }
        val cut = cut()
        cut.setGloballyEnabled(roomId, "cats", true)
        testScheduler.runCurrent()
        val otherRoom = RoomId("!other:server")
        accountData.value = ImagePackRoomsEventContent(writes.last().rooms + (otherRoom to mapOf("remote" to Unit)))
        cut.setGloballyEnabled(roomId, "cats", false)
        testScheduler.runCurrent()

        writes.last().rooms shouldBe mapOf(otherRoom to mapOf("remote" to Unit))
        accountData.value = ImagePackRoomsEventContent(emptyMap())
        cut.setGloballyEnabled(roomId, "dogs", true)
        testScheduler.runCurrent()
        writes.last().rooms shouldBe mapOf(roomId to mapOf("dogs" to Unit))
    }

    @Test
    fun `global sync restoring the original value supersedes a confirmed local write`() = runTest {
        val writes = mutableListOf<ImagePackRoomsEventContent>()
        everySuspend { userApi.setAccountData(any<ImagePackRoomsEventContent>(), userId, any()) } calls
            {
                writes.add(it.arg(0))
                Result.success(Unit)
            }
        val cut = cut()
        cut.setGloballyEnabled(roomId, "cats", true)
        testScheduler.runCurrent()
        accountData.value = writes.last()
        testScheduler.runCurrent()
        accountData.value = ImagePackRoomsEventContent(emptyMap())
        testScheduler.runCurrent()
        cut.setGloballyEnabled(roomId, "dogs", true)
        testScheduler.runCurrent()

        writes.last().rooms shouldBe mapOf(roomId to mapOf("dogs" to Unit))
    }

    @Test
    fun `intermediate global sync echo does not discard later pending writes`() = runTest {
        val writes = mutableListOf<ImagePackRoomsEventContent>()
        everySuspend { userApi.setAccountData(any<ImagePackRoomsEventContent>(), userId, any()) } calls
            {
                writes.add(it.arg(0))
                Result.success(Unit)
            }
        val cut = cut()
        cut.setGloballyEnabled(roomId, "cats", true)
        testScheduler.runCurrent()
        cut.setGloballyEnabled(roomId, "dogs", true)
        testScheduler.runCurrent()
        accountData.value = writes.first()
        cut.setGloballyEnabled(roomId, "birds", true)
        testScheduler.runCurrent()

        writes.last().rooms[roomId]?.keys shouldBe setOf("cats", "dogs", "birds")
    }

    private fun mockPackWrites(
        synced: MutableStateFlow<StateEvent<ImagePackEventContent>>,
        writes: MutableList<ImagePackEventContent>,
    ) {
        every { roomService.getState(roomId, ImagePackEventContent::class, "cats") } returns synced
        everySuspend { mediaService.prepareUploadMedia(any(), ContentType.Image.PNG) } returns "cache://sticker"
        everySuspend { mediaService.uploadMedia("cache://sticker") } returns Result.success("mxc://server/new")
        everySuspend { roomApi.sendStateEvent(roomId, any<ImagePackEventContent>(), "cats") } calls
            {
                writes.add(it.arg(1))
                Result.success(EventId("local-${writes.size}"))
            }
    }

    @Test
    fun `removes sticker from room pack`() = runTest {
        var sentContent: ImagePackEventContent? = null
        every { roomService.getState(roomId, ImagePackEventContent::class, "cats") } returns flowOf(packEvent("cats"))
        everySuspend { roomApi.sendStateEvent(roomId, any<ImagePackEventContent>(), "cats") } calls
            {
                sentContent = it.arg(1)
                Result.success(EventId("event"))
            }
        accountData.value = ImagePackRoomsEventContent(mapOf(roomId to mapOf("cats" to Unit)))
        var updatedAccountData: ImagePackRoomsEventContent? = null
        everySuspend { userApi.setAccountData(any<ImagePackRoomsEventContent>(), userId, any()) } calls
            {
                updatedAccountData = it.arg(0)
                Result.success(Unit)
            }
        val cut = cut()
        testScheduler.runCurrent()

        cut.removeImage(roomId, "cats", "wave")
        testScheduler.runCurrent()

        sentContent?.images shouldBe emptyMap()
        sentContent?.pack?.displayName shouldBe "Cats"
        updatedAccountData?.rooms shouldBe emptyMap()
    }

    @Test
    fun `removes pack with empty state key and disables global reference`() = runTest {
        every { roomService.getState(roomId, ImagePackEventContent::class, "") } returns flowOf(packEvent(""))
        accountData.value = ImagePackRoomsEventContent(mapOf(roomId to mapOf("" to Unit)))
        everySuspend { userApi.setAccountData(any<ImagePackRoomsEventContent>(), userId, any()) } returns
            Result.success(Unit)
        everySuspend { roomApi.sendStateEvent(roomId, any<ImagePackEventContent>(), "") } returns
            Result.success(EventId("removed"))
        val cut = cut()
        testScheduler.runCurrent()

        cut.removeFromRoom(roomId, "")
        testScheduler.runCurrent()

        verifySuspend {
            userApi.setAccountData(ImagePackRoomsEventContent(emptyMap()), userId, any())
            roomApi.sendStateEvent(roomId, ImagePackEventContent(images = emptyMap()), "")
        }
    }

    private fun kotlinx.coroutines.test.TestScope.cut() = requireNotNull(createViewModel())

    private fun kotlinx.coroutines.test.TestScope.createViewModel() =
        ImagePackViewModelFactoryImpl(configuration)
            .create(
                testMatrixClientViewModelContext(
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClient)) +
                                        module {
                                            single<RoomName> { roomName }
                                            single<Thumbnails> { thumbnails }
                                            single { configuration }
                                        }
                                )
                            }
                            .koin,
                    userId = userId,
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                ),
                roomId,
            )

    private fun packEvent(stateKey: String, usage: ImagePackEventContent.ImagePackMeta.Usage? = null) =
        StateEvent(
            content =
                ImagePackEventContent(
                    images =
                        mapOf(
                            "wave" to
                                ImagePackEventContent.ImagePackImage(
                                    url = "mxc://server/wave",
                                    body = "Waving cat",
                                    info = ImageInfo(width = 64, height = 64),
                                )
                        ),
                    pack = ImagePackEventContent.ImagePackMeta(displayName = "Cats", usage = usage?.let(::listOf)),
                ),
            id = EventId("$stateKey-event"),
            sender = userId,
            roomId = roomId,
            originTimestamp = 0,
            stateKey = stateKey,
        )

    private fun stickerFile(fileName: String) =
        BasicFileDescriptor(
            fileName = fileName,
            fileSize = 4,
            mimeType = ContentType.Image.PNG,
            content = byteArrayOf(1, 2, 3, 4).toByteArrayFlow(),
        )
}
