package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import dev.mokkery.answering.calls
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceAvatarTest {
    private val alice = UserId("alice", "example.org")
    private val bob = UserId("bob", "example.org")
    private val url = "mxc://example.org/avatar"
    private val calls = mutableListOf<Pair<UserId, String>>()
    private val image = CompletableDeferred<ByteArray?>()
    private var fail = false

    private val spaceId = RoomId("!space:example.org")
    private val fixtures = listOf(alice, bob).associateWith { SpaceBrowserFixture(it, spaceId) }
    private lateinit var owner: Job

    private fun TestScope.viewModel(selectedAccount: UserId? = null): SpaceContentViewModel {
        fixtures.forEach { (userId, fixture) ->
            fixture.space.value = fixture.space.value!!.copy(avatarUrl = url)
            val media = mock<MediaService>()
            everySuspend { media.getThumbnail(any(), any(), any(), any(), any(), any(), any(), any(), any()) } calls
                { args ->
                    calls += userId to args.arg<String>(0)
                    if (fail) Result.failure(IllegalStateException("thumbnail unavailable"))
                    else Result.success(InMemoryPlatformMedia(checkNotNull(image.await())))
                }
            fixture.client.di.declare<MediaService>(media)
        }
        val context = testSpaceBrowserContext(MutableStateFlow(fixtures.mapValues { it.value.client }))
        context.getKoin().declare(MatrixMessengerConfiguration())
        owner = checkNotNull(context.coroutineScope.coroutineContext[Job])
        return SpaceContentViewModelFactory.create(
            context,
            selectedAccount,
            if (selectedAccount == null) null else spaceId,
            {},
            {},
            loadHierarchy = false,
            loadDetails = false,
        )
    }

    @Test
    fun `unused avatar flows do not accumulate owner jobs`() = runTest {
        val vm = viewModel()
        val initialJobs = owner.children.count()
        repeat(1_000) { vm.avatar(alice, RoomId("!space$it:example.org")) }
        runCurrent()
        calls shouldBe emptyList()
        owner.children.count() shouldBe initialJobs
    }

    @Test
    fun `avatars load only while collected and cancel pending work on disposal`() = runTest {
        val vm = viewModel()
        val avatar = vm.avatar(alice, spaceId)
        runCurrent()
        calls shouldBe emptyList()
        val initialJobs = owner.children.count()
        val subscription = backgroundScope.launch { avatar.collect() }
        runCurrent()
        calls shouldBe listOf(alice to url)
        subscription.cancel()
        runCurrent()
        subscription.isCompleted shouldBe true
        image.isCompleted shouldBe false
        owner.children.count() shouldBe initialJobs
    }

    @Test
    fun `different accounts and avatar URLs do not share image state`() = runTest {
        val vm = viewModel()
        val first = vm.avatar(alice, spaceId)
        var firstImage: ByteArray? = null
        backgroundScope.launch { first.collect { firstImage = it } }
        image.complete(byteArrayOf(1))
        runCurrent()
        firstImage?.toList() shouldBe listOf<Byte>(1)
        val otherAccount = vm.avatar(bob, spaceId)
        backgroundScope.launch { otherAccount.collect() }
        runCurrent()
        fixtures.getValue(alice).space.value =
            fixtures.getValue(alice).space.value!!.copy(avatarUrl = "mxc://example.org/updated")
        runCurrent()
        calls.toSet() shouldBe setOf(alice to url, bob to url, alice to "mxc://example.org/updated")
    }

    @Test
    fun `failed avatar download leaves fallback available`() = runTest {
        fail = true
        val vm = viewModel()
        val avatar = vm.avatar(alice, spaceId)
        val values = mutableListOf<ByteArray?>()
        backgroundScope.launch { avatar.collect { values += it } }
        runCurrent()
        values.last() shouldBe null
        calls shouldBe listOf(alice to url)
    }

    @Test
    fun `header image follows avatar changes and removal`() = runTest {
        val vm = viewModel(alice)
        image.complete(byteArrayOf(1))
        var latest: ByteArray? = null
        backgroundScope.launch { vm.spaceImage.collect { latest = it } }
        runCurrent()
        latest?.toList() shouldBe listOf<Byte>(1)
        val space = fixtures.getValue(alice).space
        space.value = space.value!!.copy(avatarUrl = "mxc://example.org/new")
        runCurrent()
        calls shouldBe listOf(alice to url, alice to "mxc://example.org/new")
        space.value = space.value!!.copy(avatarUrl = null)
        runCurrent()
        latest shouldBe null
    }
}
