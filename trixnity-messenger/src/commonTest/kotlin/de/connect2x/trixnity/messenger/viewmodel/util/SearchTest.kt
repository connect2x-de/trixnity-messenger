package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.SearchImpl
import de.connect2x.trixnity.messenger.util.testGraphemeIterableProvider
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.job
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.client.store.UserPresence
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
import kotlin.test.Test

private fun Search.SearchUserElement.testAvatarData() = image?.let { "${userId.full}_avatarUrl" to it }

class SearchTest {

    private val myUserId = UserId("mine", "localhost")
    private val myUserData = Search.SearchUserElementImpl(
        userId = myUserId, displayName = "Me Myself", image = null, initials = "MM", presence = MutableStateFlow(null)
    )

    private val availableUsersMapping = mapOf(
        "other_local" to Search.SearchUserElementImpl(
            userId = UserId("other_local", "localhost"),
            displayName = "Other Local",
            image = null,
            initials = "OL",
            presence = MutableStateFlow(Presence.OFFLINE),
        ), "someone_remote" to Search.SearchUserElementImpl(
            userId = UserId("someone_remote", "matrix.org"),
            displayName = "Someone Remote",
            image = byteArrayOf(1, 2, 3),
            initials = "SR",
            presence = MutableStateFlow(null),
        ), "someone_with_same_prefix" to Search.SearchUserElementImpl(
            userId = UserId("someone_with_same_prefix", "localhost"),
            displayName = "Someone With Same Prefix",
            image = null,
            initials = "SWSP",
            presence = MutableStateFlow(Presence.UNAVAILABLE),
        ), "another_one" to Search.SearchUserElementImpl(
            userId = UserId("another_one", "element.io"),
            displayName = "Another One",
            image = byteArrayOf(4, 5, 6),
            initials = "AO",
            presence = MutableStateFlow(Presence.ONLINE),
        )
    )

    private val availableUsersSorted = availableUsersMapping.values.sortedBy { it.displayName }.toList()

    val matrixClientMock = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()
    val mediaServiceMock = mock<MediaService>()

    val apiClientServerMock = mock<MatrixClientServerApiClient>()
    val usersApiClientMock = mock<UserApiClient>()

    val i18n: I18n
    val search: Search

    init {
        i18n = object : I18n(
            DefaultLanguages,
            createTestMatrixMessengerSettingsHolder(),
            GetSystemLang { "en" },
            TimeZone.of("CET"),
        ) {}
        setupMatrixClient()
        setupApiMocks()
        search = SearchImpl(InitialsImpl(testGraphemeIterableProvider()), i18n, MatrixMessengerConfiguration())
    }

    @Test
    fun `clear results on empty search term`() = runTest {
        val searchTerm = myUserId.full

        injectSearchUsers(searchTerm, listOf(myUserData))
        injectSearchUsers("", listOf(myUserData))

        var res = search.searchUsers(
            coroutineScope = backgroundScope,
            matrixClient = matrixClientMock,
            searchTerm = searchTerm,
            limit = null,
        )

        res.size shouldBe 1
        res[0] shouldBeEqual myUserData

        res = search.searchUsers(
            coroutineScope = backgroundScope,
            matrixClient = matrixClientMock,
            searchTerm = "",
            limit = null,
        )

        res.size shouldBe 0
    }

    @Test
    fun `not return self on non direct search`() = runTest {
        val searchTerm = "any"

        injectSearchUsers(searchTerm, listOf(myUserData))

        val res = search.searchUsers(
            coroutineScope = backgroundScope,
            matrixClient = matrixClientMock,
            searchTerm = searchTerm,
            limit = null,
        )

        res.size shouldBe 0
    }

    @Test
    fun `not self on direct search`() = runTest {
        val searchTerm = myUserId.full

        val res = search.searchUsers(
            coroutineScope = backgroundScope,
            matrixClient = matrixClientMock,
            searchTerm = searchTerm,
            limit = null,
        )

        res.size shouldBe 1
        res[0] shouldBeEqual myUserData
    }

    @Test
    fun `limit results`() = runTest {
        val searchTerm = "limitSearch"
        val limit = availableUsersSorted.size - 1

        injectSearchUsers(searchTerm, availableUsersSorted, limit = limit.toLong())

        val res = search.searchUsers(
            coroutineScope = backgroundScope,
            matrixClient = matrixClientMock,
            searchTerm = searchTerm,
            limit = limit.toLong(),
        )

        res.size shouldBe limit
        res shouldBeEqual availableUsersSorted.take(limit)
    }

    @Test
    fun `return all available if limit is higher`() = runTest {
        val searchTerm = "limitSearch"
        val limit = availableUsersSorted.size

        injectSearchUsers(searchTerm, availableUsersSorted.drop(1), limit = limit.toLong())

        val res = search.searchUsers(
            coroutineScope = backgroundScope,
            matrixClient = matrixClientMock,
            searchTerm = searchTerm,
            limit = limit.toLong(),
        )

        res.size shouldBe limit - 1
        res shouldBeEqual availableUsersSorted.drop(1)
    }

    @Test
    fun `should get user presence`() = runTest {
        val user = availableUsersMapping["other_local"]!!
        val searchTerm = "preferUserService"

        injectSearchUsers(searchTerm, listOf(user))

        user.presence.value shouldBe Presence.OFFLINE

        val res = search.searchUsers(
            coroutineScope = backgroundScope,
            matrixClient = matrixClientMock,
            searchTerm = searchTerm,
            limit = null,
        )

        res.size shouldBe 1
        res.first().presence.drop(1).first() shouldBe Presence.OFFLINE
    }

    @Test
    fun `should get profile directly for valid userId`() = runTest {
        val user = availableUsersMapping["other_local"]!!
        val searchTerm = user.userId.full

        val res = search.searchUsers(
            coroutineScope = backgroundScope,
            matrixClient = matrixClientMock,
            searchTerm = searchTerm,
            limit = null,
        )

        res.size shouldBe 1
        res.first() shouldBeEqual user
    }


    @Test
    fun `cancel presence when cancelling searchUsersScope`() = runTest {
        val user = availableUsersMapping["other_local"]!!
        val searchTerm = "shouldCancel"
        val started = MutableStateFlow(false)
        val cancelled = MutableStateFlow(false)

        injectSearchUsers(searchTerm, listOf(user))
        every { userServiceMock.getPresence(eq(user.userId)) } returns flow {
            currentCoroutineContext().job.invokeOnCompletion { cancelled.value = true }
            started.value = true
        }

        val jobToCancel = Job()
        val scopeToCancel = backgroundScope + jobToCancel

        val subscriberJob = Job()
        val subscriberScope = backgroundScope + subscriberJob

        val res = search.searchUsers(
            coroutineScope = scopeToCancel,
            matrixClient = matrixClientMock,
            searchTerm = searchTerm,
            limit = null,
        )
        res.size shouldBe 1
        res.first().presence.value shouldBe null

        res.first().presence.launchIn(subscriberScope)

        started.first { it }
        jobToCancel.cancel()
        cancelled.first { it }
    }

    private fun injectSearchUsers(searchTerm: String, users: List<Search.SearchUserElement>, limit: Long? = null) {
        val results = users.map {
            SearchUsers.Response.SearchUser(avatarUrl = it.testAvatarData()?.first, it.displayName, it.userId)
        }
        everySuspend {
            usersApiClientMock.searchUsers(
                eq(searchTerm), acceptLanguage = i18n.currentLang.code, limit
            )
        } returns Result.success(SearchUsers.Response(limited = limit != null, results = results))
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
                })
        }.koin

        every { matrixClientMock.userId } returns myUserId
        every { matrixClientMock.api } returns apiClientServerMock
        every { apiClientServerMock.user } returns usersApiClientMock
        every { userServiceMock.getPresence(any()) } returns flowOf(
            UserPresence(Presence.OFFLINE, Clock.System.now())
        )
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
                    })
            )
        }
    }
}
