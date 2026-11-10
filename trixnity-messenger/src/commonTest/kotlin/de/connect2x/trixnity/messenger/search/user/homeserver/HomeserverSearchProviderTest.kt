package de.connect2x.trixnity.messenger.search.user.homeserver

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.UserApiClient
import de.connect2x.trixnity.core.model.Profile
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.search.provider.SearchProviderResult
import de.connect2x.trixnity.messenger.search.user.UserSearchContext
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class HomeserverSearchProviderTest {
    private val account = UserId("@me:local")
    private val userId = UserId("@invitee:remote")

    @Test
    fun `failed discovery and profile lookup return selectable user with warning`() = runTest {
        val provider = provider(discoverySucceeds = false, profile = Result.failure(IllegalStateException("offline")))
        val result =
            provider
                .search(userId.full, emptyList(), UserSearchContext(account), backgroundScope)
                .shouldBeInstanceOf<SearchProviderResult.Success<HomeserverUserSearchResult>>()
        result.result.single().userId shouldBe userId
        result.result.single().warning shouldBe "Could not verify remote. This Matrix user ID might not be valid."
    }

    @Test
    fun `successful profile lookup suppresses discovery warning`() = runTest {
        val provider = provider(discoverySucceeds = false, profile = Result.success(Profile()))
        val result =
            provider
                .search(userId.full, emptyList(), UserSearchContext(account), backgroundScope)
                .shouldBeInstanceOf<SearchProviderResult.Success<HomeserverUserSearchResult>>()
        result.result.single().userId shouldBe userId
        result.result.single().warning shouldBe null
    }

    @Test
    fun `successful discovery returns user without warning when profile is unavailable`() = runTest {
        val provider = provider(discoverySucceeds = true, profile = Result.failure(IllegalStateException("forbidden")))
        val result =
            provider
                .search(userId.full, emptyList(), UserSearchContext(account), backgroundScope)
                .shouldBeInstanceOf<SearchProviderResult.Success<HomeserverUserSearchResult>>()
        result.result.single().userId shouldBe userId
        result.result.single().warning shouldBe null
    }

    private fun provider(discoverySucceeds: Boolean, profile: Result<Profile>): HomeserverSearchProvider {
        val client = mock<MatrixClient>()
        val clients = mock<MatrixClients>()
        val api = mock<MatrixClientServerApiClient>()
        val userApi = mock<UserApiClient>()
        val userService = mock<UserService>()
        val initials = mock<Initials>()
        val i18n =
            I18n(DefaultLanguages, createTestMatrixMessengerSettingsHolder(), GetSystemLang { "en" }, TimeZone.UTC)
        every { clients.value } returns mapOf(account to client)
        every { client.api } returns api
        every { api.user } returns userApi
        every { client.di } returns koinApplication { modules(module { single { userService } }) }.koin
        every { userService.getPresence(userId) } returns flowOf(null)
        everySuspend { userApi.getProfile(userId) } returns profile
        every { initials.compute(any()) } returns "I"
        return HomeserverSearchProvider(
            initials,
            i18n,
            clients,
            MatrixMessengerConfiguration(
                httpClientEngine =
                    MockEngine { request ->
                        if (discoverySucceeds && request.url.encodedPath == "/_matrix/client/versions") {
                            respond(
                                """{"versions":["v1.1"]}""",
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        } else {
                            respond(
                                "{}",
                                HttpStatusCode.NotFound,
                                headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                    }
            ),
        )
    }
}
