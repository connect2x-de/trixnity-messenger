package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds


class BlockedContactsSettingsViewModelTest {
    val matrixClientMock1 = mock<MatrixClient>()

    val matrixClientMock2 = mock<MatrixClient>()

    val userBlockingMock = mock<UserBlocking>()

    private val contact1 = UserId("jerk", "localhost")
    private val contact2 = UserId("another_jerk", "localhost")
    private val contact3 = UserId("do_not_want", "localhost")

    private val blockedContactsForUser1 = MutableStateFlow(listOf<UserId>())

    init {
        blockedContactsForUser1.value = listOf(
            contact3,
            contact1,
        )
        resetMocks(matrixClientMock1, matrixClientMock2, userBlockingMock)
        every { userBlockingMock.getBlockedUsers(matrixClientMock1) } returns blockedContactsForUser1

        every { userBlockingMock.getBlockedUsers(matrixClientMock2) } returns flowOf(listOf(contact1, contact2))
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `return a list and count of blocked contacts per account`() = runTest {
        val viewModel1 = blockedContactsSettingsViewModel(User.USER1)
        backgroundScope.launch { viewModel1.blockedContactsList.collect() }
        backgroundScope.launch { viewModel1.blockedContactsCount.collect() }
        eventually(1.seconds) {
            viewModel1.blockedContactsList.value shouldBe listOf(
                BlockedContact(contact3, false),
                BlockedContact(contact1, false),
            )
            viewModel1.blockedContactsCount.value shouldBe 2
        }

        val viewModel2 = blockedContactsSettingsViewModel(User.USER2)
        backgroundScope.launch { viewModel2.blockedContactsList.collect() }
        backgroundScope.launch { viewModel2.blockedContactsCount.collect() }
        eventually(1.seconds) {
            viewModel2.blockedContactsList.value shouldBe listOf(
                BlockedContact(contact1, false),
                BlockedContact(contact2, false),
            )
            viewModel2.blockedContactsCount.value shouldBe 2
        }
    }

    @Test
    fun `mark a blocked contact as unblocking and then unblock them`() = runTest {
        val isRequestCompleted = MutableStateFlow(false)
        everySuspend {
            userBlockingMock.unblockUser(
                matrixClientMock1,
                contact3,
                any(),
                any(),
            )
        } calls { isRequestCompleted.first { it } }

        val viewModel = blockedContactsSettingsViewModel(User.USER1)
        backgroundScope.launch { viewModel.blockedContactsList.collect() }
        yield()

        eventually(1.seconds) {
            viewModel.blockedContactsList.value shouldBe listOf(
                BlockedContact(contact3, false),
                BlockedContact(contact1, false),
            )
        }

        viewModel.unblockContact(contact3)
        yield()

        eventually(1.seconds) {
            viewModel.blockedContactsList.value shouldBe listOf(
                BlockedContact(contact3, true),
                BlockedContact(contact1, false),
            )
        }

        blockedContactsForUser1.value = listOf(
            contact1,
        )
        isRequestCompleted.value = true
        eventually(1.seconds) {
            viewModel.blockedContactsList.value shouldBe listOf(
                BlockedContact(contact1, false),
            )
        }
    }

    private fun TestScope.blockedContactsSettingsViewModel(
        user: User,
    ): BlockedContactsSettingsViewModel {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(
                        User.USER1.userId to matrixClientMock1,
                        User.USER2.userId to matrixClientMock2,
                    )
                ) + module {
                    single { userBlockingMock }
                })
        }.koin
        return BlockedContactsSettingsViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                userId = user.userId,
                di = di,
            ),
            onCloseBlockedContactsSettings = mock(),
        )
    }

    enum class User(val userId: UserId) {
        USER1(UserId("test1", "server")), USER2(UserId("test2", "server")),
    }
}
