package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.SearchImpl
import dev.mokkery.answering.returns
import dev.mokkery.coroutines.answering.awaits
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.clientserverapi.model.users.GetProfile
import net.folivo.trixnity.clientserverapi.model.users.SearchUsers
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration

private fun Search.SearchUserElement.testAvatarData() = image?.let { "${userId.full}_avatarUrl" to it }

@OptIn(ExperimentalCoroutinesApi::class)
class SearchTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    private val myUserId = UserId("mine", "localhost")
    private val myUserData = Search.SearchUserElementImpl(
        userId = myUserId,
        displayName = "Me Myself",
        image = null,
        initials = "MM",
        presence = MutableStateFlow(null)
    )

    private val availableUsersMapping = mapOf(
        "other_local" to Search.SearchUserElementImpl(
            userId = UserId("other_local", "localhost"),
            displayName = "Other Local",
            image = null,
            initials = "OL",
            presence = MutableStateFlow(Presence.OFFLINE),
        ),
        "someone_remote" to Search.SearchUserElementImpl(
            userId = UserId("someone_remote", "matrix.org"),
            displayName = "Someone Remote",
            image = byteArrayOf(1, 2, 3),
            initials = "SR",
            presence = MutableStateFlow(null),
        ),
        "someone_with_same_prefix" to Search.SearchUserElementImpl(
            userId = UserId("someone_with_same_prefix", "localhost"),
            displayName = "Someone With Same Prefix",
            image = null,
            initials = "SWSP",
            presence = MutableStateFlow(Presence.UNAVAILABLE),
        ),
        "another_one" to Search.SearchUserElementImpl(
            userId = UserId("another_one", "element.io"),
            displayName = "Another One",
            image = byteArrayOf(4, 5, 6),
            initials = "AO",
            presence = MutableStateFlow(Presence.ONLINE),
        )
    )

    private val availableUsersSorted = availableUsersMapping.values.sortedBy { it.displayName }.toList()

    @OptIn(ExperimentalCoroutinesApi::class)
    val testDispatcher = UnconfinedTestDispatcher()
    val defaultPresenceScope = TestScope(testDispatcher)

    val matrixClientMock = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()
    val mediaServiceMock = mock<MediaService>()

    val apiClientServerMock = mock<MatrixClientServerApiClient>()
    val usersApiClientMock = mock<UserApiClient>()

    val userPresence = MutableStateFlow<Map<UserId, PresenceEventContent>>(mapOf())

    lateinit var i18n: I18n
    lateinit var search: Search

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(
                matrixClientMock,
                userServiceMock,
                mediaServiceMock,
                apiClientServerMock,
                usersApiClientMock,
            )
            userPresence.update { emptyMap() }
            setupI18N()
            setupMatrixClient()
            setupApiMocks()
            search = SearchImpl(Initials, i18n)
        }

        should("be sorted by display name") {
            val searchTerm = "anything"

            injectSearchUsers(searchTerm, availableUsersSorted.shuffled())

            val res = search.searchUsers(
                matrixClientMock,
                searchTerm,
                limit = null,
                presenceScope = defaultPresenceScope,
                maxPreviewSize = Long.MAX_VALUE
            )

            res.size shouldBe availableUsersSorted.size
            res shouldBeEqual availableUsersSorted

            cancelNeverEndingCoroutines()
        }

        should("not return self on non direct search") {
            val searchTerm = "any"

            injectSearchUsers(searchTerm, listOf(myUserData))

            val res = search.searchUsers(
                matrixClientMock,
                searchTerm,
                limit = null,
                presenceScope = defaultPresenceScope,
                maxPreviewSize = Long.MAX_VALUE
            )

            res.size shouldBe 0

            cancelNeverEndingCoroutines()
        }

        should("not self on direct search") {
            val searchTerm = myUserId.full

            val res = search.searchUsers(
                matrixClientMock,
                searchTerm,
                limit = null,
                presenceScope = defaultPresenceScope,
                maxPreviewSize = Long.MAX_VALUE
            )

            res.size shouldBe 1
            res[0] shouldBeEqual myUserData

            cancelNeverEndingCoroutines()
        }

        should("limit results") {
            val searchTerm = "limitSearch"
            val limit = availableUsersSorted.size - 1

            injectSearchUsers(searchTerm, availableUsersSorted, limit = limit.toLong())

            val res = search.searchUsers(
                matrixClientMock,
                searchTerm,
                limit = limit.toLong(),
                presenceScope = defaultPresenceScope,
                maxPreviewSize = Long.MAX_VALUE
            )

            res.size shouldBe limit
            res shouldBeEqual availableUsersSorted.take(limit)

            cancelNeverEndingCoroutines()
        }

        should("return all available if limit is higher") {
            val searchTerm = "limitSearch"
            val limit = availableUsersSorted.size

            injectSearchUsers(searchTerm, availableUsersSorted.drop(1), limit = limit.toLong())

            val res = search.searchUsers(
                matrixClientMock,
                searchTerm,
                limit = limit.toLong(),
                presenceScope = defaultPresenceScope,
                maxPreviewSize = Long.MAX_VALUE
            )

            res.size shouldBe limit - 1
            res shouldBeEqual availableUsersSorted.drop(1)

            cancelNeverEndingCoroutines()
        }

        should("prefer userService over api") {
            val user = availableUsersMapping["other_local"]!!
            val searchTerm = "preferUserService"

            userPresence.update { mapOf(user.userId to PresenceEventContent(Presence.ONLINE)) }
            injectSearchUsers(searchTerm, listOf(user))

            user.presence.value shouldBe Presence.OFFLINE

            val res = search.searchUsers(
                matrixClientMock,
                searchTerm,
                limit = null,
                presenceScope = this,
                maxPreviewSize = Long.MAX_VALUE
            )

            res.size shouldBe 1
            res.first().presence.drop(1).first() shouldBe Presence.ONLINE

            cancelNeverEndingCoroutines()
        }

        should("should get profile directly for valid userId") {
            val user = availableUsersMapping["other_local"]!!
            val searchTerm = user.userId.full

            val res = search.searchUsers(
                matrixClientMock,
                searchTerm,
                limit = null,
                presenceScope = defaultPresenceScope,
                maxPreviewSize = Long.MAX_VALUE
            )

            res.size shouldBe 1
            res.first() shouldBeEqual user

            cancelNeverEndingCoroutines()
        }



        should("cancel presence when cancelling searchUsersScope") {
            val user = availableUsersMapping["other_local"]!!
            val searchTerm = "shouldCancel"
            val blockingState = Channel<BlockingPresenceState>(1)

            injectSearchUsers(searchTerm, listOf(user))
            everySuspend { usersApiClientMock.getPresence(eq(user.userId), any()) } awaits {
                BlockingPresenceResponse {
                    blockingState.trySend(it)
                }
            }

            val jobToCancel = Job()
            val scopeToCancel = TestScope(UnconfinedTestDispatcher() + jobToCancel)

            val subscriberJob = Job()
            val subscriberScope = TestScope(UnconfinedTestDispatcher() + subscriberJob)

            val res = search.searchUsers(
                matrixClientMock,
                searchTerm,
                limit = null,
                presenceScope = scopeToCancel,
                maxPreviewSize = Long.MAX_VALUE
            )
            res.size shouldBe 1
            res.first().presence.value shouldBe null

            res.first().presence.launchIn(subscriberScope)

            blockingState.receive() shouldBe BlockingPresenceState.STARTED
            jobToCancel.cancel()
            blockingState.receive() shouldBe BlockingPresenceState.FINISHED
        }
    }

    private fun injectSearchUsers(searchTerm: String, users: List<Search.SearchUserElement>, limit: Long? = null) {
        val results = users.map {
            SearchUsers.Response.SearchUser(avatarUrl = it.testAvatarData()?.first, it.displayName, it.userId)
        }
        everySuspend {
            usersApiClientMock.searchUsers(
                eq(searchTerm),
                acceptLanguage = i18n.currentLang.code,
                limit
            )
        } returns
                Result.success(SearchUsers.Response(limited = limit != null, results = results))
    }

    private fun setupI18N() {
        i18n = object : I18n(
            DefaultLanguages,
            createTestMatrixMessengerSettingsHolder(),
            GetSystemLang { "en" },
            TimeZone.of("CET"),
        ) {}
    }

    private fun setupApiMocks() {
        val mine = listOf("mine" to myUserData)
        (availableUsersMapping + mine).forEach { (_, data) ->
            val avatarData = data.testAvatarData()

            setupGetProfile(data.userId, data.displayName, avatarData?.first)
            setupGetPresence(data.userId, data.presence)
            setupGetThumbnail(avatarData)
        }
    }

    private fun setupMatrixClient() {
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { userServiceMock }
                    single { mediaServiceMock }
                }
            )
        }.koin

        every { matrixClientMock.userId } returns myUserId
        every { matrixClientMock.api } returns apiClientServerMock
        every { apiClientServerMock.user } returns usersApiClientMock

        every { userServiceMock.userPresence } returns userPresence
    }

    private fun setupGetProfile(userId: UserId, displayName: String, avatarUrl: String? = null) {
        everySuspend { usersApiClientMock.getProfile(eq(userId)) } returns Result.success(
            GetProfile.Response(
                displayName = displayName,
                avatarUrl = avatarUrl,
            )
        )
    }

    private fun setupGetPresence(userId: UserId, presence: StateFlow<Presence?>) {
        when (val p = presence.value) {
            null -> {
                everySuspend { usersApiClientMock.getPresence(eq(userId), any()) } returns Result.failure(
                    Exception("presence not available")
                )
            }

            else -> {
                everySuspend { usersApiClientMock.getPresence(eq(userId), any()) } returns Result.success(
                    PresenceEventContent(p)
                )
            }
        }
    }

    private fun setupGetThumbnail(data: Pair<String, ByteArray>?) {
        data?.also { (url, bytes) ->
            everySuspend { mediaServiceMock.getThumbnail(eq(url), any(), any()) } returns Result.success(
                InMemoryPlatformMedia(
                    flow {
                        bytes.forEach { emit(byteArrayOf(it)) }
                    }
                )
            )
        }
    }
}

private enum class BlockingPresenceState {
    STARTED, FINISHED
}

@OptIn(InternalForInheritanceCoroutinesApi::class)
private class BlockingPresenceResponse(
    private val inner: CompletableDeferred<Result<PresenceEventContent>> = CompletableDeferred(),
    val onChange: (BlockingPresenceState) -> Unit,
) : Deferred<Result<PresenceEventContent>> by inner {
    override suspend fun await(): Result<PresenceEventContent> {
        currentCoroutineContext().job.invokeOnCompletion {
            if (it is CancellationException) {
                inner.cancel(it)
                onChange(BlockingPresenceState.FINISHED)
            }
        }
        onChange(BlockingPresenceState.STARTED)
        delay(Duration.INFINITE)
        return Result.failure(Exception("unreachable"))
    }
}
