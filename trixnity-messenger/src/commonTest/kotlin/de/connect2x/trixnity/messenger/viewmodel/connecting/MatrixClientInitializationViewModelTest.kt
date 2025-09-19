package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.continually
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.update
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class MatrixClientInitializationViewModelTest {
    val matrixClientsMock = mock<MatrixClients>()

    private val onNoAccountsMock = mock<Function0<Unit>>()
    private lateinit var cut: MatrixClientInitializationViewModel

    init {
        resetMocks(matrixClientsMock, onNoAccountsMock)
        every { onNoAccountsMock.invoke() } returns Unit
        everySuspend { matrixClientsMock.initFromStoreResult } returns MutableStateFlow(
            MatrixClients.InitFromStoreResult(
                success = emptySet(),
                failures = emptyMap(),
            )
        )
    }

    @Test
    fun `call onNoAccounts when no accounts are present`() = runTest {
        matrixClientInitializationViewModel(
            accounts = emptyMap(), selectedAccount = null
        )
        eventually(4.seconds) {
            verify { onNoAccountsMock.invoke() }
        }
    }

    @Test
    fun `leave the currently active account if the account can be found`() = runTest {
        val settings = matrixClientInitializationViewModel(
            accounts = mapOf(
                UserId(
                    "user1", "local.local"
                ) to MatrixMessengerAccountSettingsBase.withConfigDefaults(
                    config = MatrixMessengerConfiguration(),
                    displayColor = null,
                ),
            ), selectedAccount = UserId(
                "user1", "local.local"
            )
        )
        continually(2.seconds) {
            settings.value.base.accounts.size shouldBe 1
        }
    }

    @Test
    fun `select the only left account when the currently active account is not present anymore`() = runTest {
        val settings = matrixClientInitializationViewModel(
            accounts = mapOf(
                UserId(
                    "user1", "local.local"
                ) to MatrixMessengerAccountSettingsBase.withConfigDefaults(
                    config = MatrixMessengerConfiguration(),
                    displayColor = null,
                ),
            ), selectedAccount = UserId("user2", "local.local")
        )
        eventually(2.seconds) {
            settings.value.base.selectedAccount shouldBe UserId("user1", "local.local")
        }
    }

    @Test
    fun `select all accounts if the currently active account is not present anymore`() = runTest {
        val settings = matrixClientInitializationViewModel(
            accounts = mapOf(
                UserId(
                    "user1", "local.local"
                ) to MatrixMessengerAccountSettingsBase.withConfigDefaults(
                    config = MatrixMessengerConfiguration(),
                    displayColor = null,
                ),
                UserId(
                    "user2", "local.local"
                ) to MatrixMessengerAccountSettingsBase.withConfigDefaults(
                    config = MatrixMessengerConfiguration(),
                    displayColor = null,
                ),
            ), selectedAccount = UserId("user666", "local.local")
        )
        eventually(2.seconds) {
            settings.value.base.selectedAccount shouldBe null
        }
    }

    private suspend fun TestScope.matrixClientInitializationViewModel(
        accounts: Map<UserId, MatrixMessengerAccountSettingsBase>, selectedAccount: UserId?
    ): MatrixMessengerSettingsHolder {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules() + module {
                    single<MatrixClients> { matrixClientsMock }
                })
        }.koin
        val settings = di.get<MatrixMessengerSettingsHolder>()
        accounts.forEach { (account, accountSettings) ->
            settings.create(account, accountSettings)
        }
        settings.update<MatrixMessengerSettingsBase>() { it.copy(selectedAccount = selectedAccount) }
        val viewModelContext = testViewModelContext(di)
        // prevent GC to clean up the viewmodel
        cut = MatrixClientInitializationViewModelImpl(
            viewModelContext = viewModelContext,
            onNoAccounts = onNoAccountsMock,
            onInitializationSuccess = {},
            onInitializationFailure = { _, _ -> },
        )
        return settings
    }
}
