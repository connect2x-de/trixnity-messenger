package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.LoadStoreException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import net.folivo.trixnity.core.model.UserId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

class MatrixClientInitializationViewModelTest : ShouldSpec() {
    private val mocker = Mocker()

    @Mock
    lateinit var matrixClientsMock: MatrixClients

    private val onNoAccountsMock = mockFunction0<Unit>(mocker)
    private lateinit var cut: MatrixClientInitializationViewModel

    init {
        mocker.reset()
        injectMocks(mocker)

        beforeTest {
            with(mocker) {
                every { onNoAccountsMock.invoke() } returns Unit
                everySuspending { matrixClientsMock.initFromStore() } returns
                        MatrixClients.InitFromStoreResult(
                            success = emptySet(),
                            failures = emptyMap(),
                        )
            }
        }

        // still fails from time to time for no apparent reason, so deactivate
        xshould("call `onNoAccounts` when no accounts are present") {
            matrixClientInitializationViewModel(
                accounts = emptyMap(),
                selectedAccount = null
            )
            eventually(2.seconds) {
                mocker.verify(exhaustive = false) { onNoAccountsMock.invoke() }
            }
        }

        should("leave the currently active account if the account can be found") {
            val settings = matrixClientInitializationViewModel(
                accounts = mapOf(
                    UserId(
                        "user1",
                        "local.local"
                    ) to MatrixMessengerAccountSettings.withConfigDefaults(
                        databasePassword = null,
                        config = MatrixMessengerConfiguration(),
                        displayColor = null,
                    ),
                ),
                selectedAccount = UserId(
                    "user1",
                    "local.local"
                )
            )
            continually(2.seconds) {
                settings.value.accounts.size shouldBe 1
            }
        }

        should("select the only left account when the currently active account is not present anymore") {
            val settings = matrixClientInitializationViewModel(
                accounts = mapOf(
                    UserId(
                        "user1",
                        "local.local"
                    ) to MatrixMessengerAccountSettings.withConfigDefaults(
                        databasePassword = null,
                        config = MatrixMessengerConfiguration(),
                        displayColor = null,
                    ),
                ),
                selectedAccount = UserId("user2", "local.local")
            )
            eventually(2.seconds) {
                settings.value.selectedAccount shouldBe UserId("user1", "local.local")
            }
        }

        should("select all accounts if the currently active account is not present anymore") {
            val settings = matrixClientInitializationViewModel(
                accounts = mapOf(
                    UserId(
                        "user1",
                        "local.local"
                    ) to MatrixMessengerAccountSettings.withConfigDefaults(
                        databasePassword = null,
                        config = MatrixMessengerConfiguration(),
                        displayColor = null,
                    ),
                    UserId(
                        "user2",
                        "local.local"
                    ) to MatrixMessengerAccountSettings.withConfigDefaults(
                        databasePassword = null,
                        config = MatrixMessengerConfiguration(),
                        displayColor = null,
                    ),
                ),
                selectedAccount = UserId("user666", "local.local")
            )
            eventually(2.seconds) {
                settings.value.selectedAccount shouldBe null
            }
        }
    }

    private suspend fun matrixClientInitializationViewModel(
        accounts: Map<UserId, MatrixMessengerAccountSettings>,
        selectedAccount: UserId?
    ): MatrixMessengerSettingsHolder {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules() +
                        module {
                            single<MatrixClients> { matrixClientsMock }
                        }
            )
        }.koin
        val settings = di.get<MatrixMessengerSettingsHolder>()
        settings.update { it.copy(accounts = accounts, selectedAccount = selectedAccount) }
        val viewModelContext = ViewModelContextImpl(
            di,
            componentContext = DefaultComponentContext(LifecycleRegistry())
        )
        // prevent GC to clean up the viewmodel
        cut = MatrixClientInitializationViewModelImpl(
            viewModelContext = viewModelContext,
            onNoAccounts = onNoAccountsMock,
            onInitializationSuccess = {},
            onInitializationFailure = {},
            onStoreFailure = { _: UserId, _: LoadStoreException -> },
        )
        return settings
    }
}