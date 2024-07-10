package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds


class BlockedContactsSettingsViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 3_000

    val matrixClientMock1 = mock<MatrixClient>()

    val matrixClientMock2 = mock<MatrixClient>()

    val userBlockingMock = mock<UserBlocking>()

    private val contact1 = UserId("jerk", "localhost")
    private val contact2 = UserId("another_jerk", "localhost")
    private val contact3 = UserId("do_not_want", "localhost")

    private val blockedContactsForUser1 = MutableStateFlow(listOf<UserId>())

    init {

        beforeTest {
            blockedContactsForUser1.value = listOf(
                contact3,
                contact1,
            )
            resetMocks(matrixClientMock1, matrixClientMock2, userBlockingMock)
            every { userBlockingMock.getBlockedUsers(eq(matrixClientMock1)) } returns
                    blockedContactsForUser1

            every { userBlockingMock.getBlockedUsers(eq(matrixClientMock2)) } returns
                    flowOf(listOf(contact1, contact2))
        }

        should("return a list and count of blocked contacts per account") {
            val viewModel1 = blockedContactsSettingsViewModel(coroutineContext, User.USER1)
            launch { viewModel1.blockedContactsList.collect() }
            launch { viewModel1.blockedContactsCount.collect() }
            eventually(1.seconds) {
                viewModel1.blockedContactsList.value shouldBe listOf(
                    BlockedContact(contact3, false),
                    BlockedContact(contact1, false),
                )
                viewModel1.blockedContactsCount.value shouldBe 2
            }

            val viewModel2 = blockedContactsSettingsViewModel(coroutineContext, User.USER2)
            launch { viewModel2.blockedContactsList.collect() }
            launch { viewModel2.blockedContactsCount.collect() }
            eventually(1.seconds) {
                viewModel2.blockedContactsList.value shouldBe listOf(
                    BlockedContact(contact1, false),
                    BlockedContact(contact2, false),
                )
                viewModel2.blockedContactsCount.value shouldBe 2
            }

            cancelNeverEndingCoroutines()
        }

        should("mark a blocked contact as unblocking and then unblock them") {
            val isRequestCompleted = MutableStateFlow(false)
            everySuspend {
                userBlockingMock.unblockUser(
                    eq(matrixClientMock1),
                    eq(contact3),
                    any(),
                    any(),
                )
            } calls { isRequestCompleted.first { it } }

            val viewModel = blockedContactsSettingsViewModel(coroutineContext, User.USER1)
            launch { viewModel.blockedContactsList.collect() }
            eventually(1.seconds) {
                viewModel.blockedContactsList.value shouldBe listOf(
                    BlockedContact(contact3, false),
                    BlockedContact(contact1, false),
                )
            }

            viewModel.unblockContact(contact3)
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

            cancelNeverEndingCoroutines()
        }
    }

    private fun blockedContactsSettingsViewModel(
        coroutineContext: CoroutineContext,
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
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                coroutineContext = coroutineContext,
                userId = user.userId,
                di = di,
            ),
            onCloseBlockedContactsSettings = mock(),
        )
    }

    enum class User(val userId: UserId) {
        USER1(UserId("test1", "server")),
        USER2(UserId("test2", "server")),
    }
}
