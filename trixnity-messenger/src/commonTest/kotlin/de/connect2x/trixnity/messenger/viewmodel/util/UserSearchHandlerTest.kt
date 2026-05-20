package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.DefaultUserSearchHandler
import de.connect2x.trixnity.messenger.util.Search
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class UserSearchHandlerTest {

    val searchMock = mock<Search>()
    val matrixClientMock = mock<MatrixClient>()

    private val userId = "@user:local.local"
    private val searchUser = Search.SearchUserElementImpl("The user", "Tu", null, UserId(userId))

    @BeforeTest
    fun setup() {
        configureTestLogging()
        resetMocks(searchMock, matrixClientMock)
        everySuspend { searchMock.searchUsers(any(), matrixClientMock, userId, any()) } returns listOf(searchUser)
    }

    @Test
    fun `should return empty list when the search term is blank`() = runTest {
        val cut = defaultUserSearchHandler()
        cut.searchTerm.update("  \t ")
        delay(400.milliseconds) // debounce is 300 ms

        cut.foundUsers.value shouldBe listOf()
    }

    @Test
    fun `should search for Matrix IDs with lowercase`() = runTest {
        val cut = defaultUserSearchHandler()
        cut.searchTerm.update("@USER:LocAl.local")
        delay(400.milliseconds) // debounce is 300 ms

        cut.foundUsers.value shouldBe listOf(searchUser)
    }

    @Test
    fun `should search for Matrix IDs with lowercase and whitespace`() = runTest {
        val cut = defaultUserSearchHandler()
        cut.searchTerm.update("  @USER:LocAl.local  \t")
        delay(400.milliseconds) // debounce is 300 ms

        cut.foundUsers.value shouldBe listOf(searchUser)
    }

    @Test
    fun `should return found users`(): TestResult = runTest {
        val otherUser = Search.SearchUserElementImpl("Other user", "Ou", null, UserId("ou:local.local"))
        everySuspend { searchMock.searchUsers(any(), matrixClientMock, "us", any()) } returns
            listOf(searchUser, otherUser)
        val cut = defaultUserSearchHandler()
        cut.searchTerm.update("us")
        delay(400.milliseconds) // debounce is 300 ms

        cut.foundUsers.value shouldBe listOf(searchUser, otherUser)
    }

    @Test
    fun `should not return users that are already selected`() = runTest {
        val otherUser = Search.SearchUserElementImpl("Other user", "Ou", null, UserId("ou:local.local"))
        everySuspend { searchMock.searchUsers(any(), matrixClientMock, "us", any()) } returns
            listOf(searchUser, otherUser)
        val cut = defaultUserSearchHandler()
        cut.searchTerm.update("us")
        cut.selectUser(searchUser)
        delay(400.milliseconds) // debounce is 300 ms

        cut.foundUsers.value shouldBe listOf(otherUser)
    }

    @Test
    fun `should not return users that are in the filter list`() = runTest {
        val otherUser = Search.SearchUserElementImpl("Other user", "Ou", null, UserId("ou:local.local"))
        everySuspend { searchMock.searchUsers(any(), matrixClientMock, "us", any()) } returns
            listOf(searchUser, otherUser)
        val cut = defaultUserSearchHandler(filterNotUsers = flowOf(setOf(UserId("ou:local.local"))))
        cut.searchTerm.update("us")
        delay(400.milliseconds) // debounce is 300 ms

        cut.foundUsers.value shouldBe listOf(searchUser)
    }

    fun TestScope.defaultUserSearchHandler(
        filterNotUsers: Flow<Set<UserId>> = flowOf(setOf())
    ): DefaultUserSearchHandler {
        val result =
            DefaultUserSearchHandler(
                coroutineScope = backgroundScope,
                search = searchMock,
                client = matrixClientMock,
                filterNotUsers = filterNotUsers,
            )
        backgroundScope.launch { result.foundUsers.collect() }
        return result
    }
}
