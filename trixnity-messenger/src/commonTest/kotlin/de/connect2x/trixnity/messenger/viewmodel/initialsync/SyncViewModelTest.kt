package de.connect2x.trixnity.messenger.viewmodel.initialsync

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.IsNetworkAvailable
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import io.kotest.assertions.timing.continually
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

class SyncViewModelTest : ShouldSpec() {

    override fun timeout(): Long = 5_000

    private val mocker = Mocker()

    @Mock
    lateinit var onSyncDone: () -> Unit

    private var initialSyncWasCalled = false
    private var onSyncDoneCalled = false

    init {
        coroutineTestScope = true
        Dispatchers.setMain(testMainDispatcher)
        isolationMode = IsolationMode.InstancePerTest // initialSyncWasCalled has to be reset

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { onSyncDone.invoke() } runs {
                    onSyncDoneCalled = true
                }
            }
        }

        should("not do any sync if there is no network") {
            val cut = syncViewModel(mapOf("test1" to InitialSyncState.NOT_DONE), isNetworkAvailable = false)
            continually(1.seconds) {
                initialSyncWasCalled shouldBe false
                cut.accountSyncStates.value shouldBe mapOf(
                    "test1" to AccountSync(
                        InitialSyncState.NOT_DONE,
                        AccountSyncState.RUNNING,
                    )
                )
            }
            onSyncDoneCalled shouldBe true
        }

        should("do the sync for all accounts if the network is available") {
            val cut = syncViewModel(
                accountNames = mapOf("test1" to InitialSyncState.NOT_DONE, "test2" to InitialSyncState.DONE),
                runInitialSync = mapOf("test1" to flowOf(true), "test2" to flowOf(false)),
                isNetworkAvailable = true,
            )
            eventually(1.seconds) {
                initialSyncWasCalled shouldBe true
                cut.accountSyncStates.value shouldBe mapOf(
                    "test1" to AccountSync(
                        InitialSyncState.NOT_DONE,
                        AccountSyncState.DONE,
                    ),
                    "test2" to AccountSync(
                        InitialSyncState.DONE,
                        AccountSyncState.DONE,
                    ),
                )
                onSyncDoneCalled shouldBe true
            }
        }

        should("finish the syncs for different accounts in parallel") {
            val cut = syncViewModel(
                accountNames = mapOf("test1" to InitialSyncState.NOT_DONE, "test2" to InitialSyncState.DONE),
                runInitialSync = mapOf("test1" to flow {
                    delay(2.seconds)
                    emit(true)
                }, "test2" to flowOf(false)),
                isNetworkAvailable = true,
            )
            eventually(1.seconds) {
                initialSyncWasCalled shouldBe true
                cut.accountSyncStates.value shouldBe mapOf(
                    "test1" to AccountSync(
                        InitialSyncState.NOT_DONE,
                        AccountSyncState.RUNNING,
                    ),
                    "test2" to AccountSync(
                        InitialSyncState.DONE,
                        AccountSyncState.DONE,
                    ),
                )
                onSyncDoneCalled shouldBe false
            }
            eventually(3.seconds) {
                initialSyncWasCalled shouldBe true
                cut.accountSyncStates.value shouldBe mapOf(
                    "test1" to AccountSync(
                        InitialSyncState.NOT_DONE,
                        AccountSyncState.DONE,
                    ),
                    "test2" to AccountSync(
                        InitialSyncState.DONE,
                        AccountSyncState.DONE,
                    ),
                )
                onSyncDoneCalled shouldBe true
            }
        }

        should("cancel syncs for all accounts if cancellation is called") {
            val cut = syncViewModel(
                accountNames = mapOf("test1" to InitialSyncState.NOT_DONE, "test2" to InitialSyncState.DONE),
                runInitialSync = mapOf("test1" to flow {
                    delay(2.seconds)
                    emit(true)
                }, "test2" to flow {
                    delay(2.seconds)
                    emit(false)
                }),
                isNetworkAvailable = true,
            )
            cut.cancel()
            onSyncDoneCalled shouldBe true // the sync is considered DONE even when cancelled
        }
    }

    fun syncViewModel(
        accountNames: Map<String, InitialSyncState>,
        runInitialSync: Map<String, Flow<Boolean>> = mapOf(),
        isNetworkAvailable: Boolean = true,
    ): SyncViewModel =
        SyncViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                di = di(isNetworkAvailable, runInitialSync),
                componentContext = DefaultComponentContext(LifecycleRegistry()),
            ),
            accountNames = accountNames,
            onSyncDone = onSyncDone,
        )

    private fun di(isNetworkAvailable: Boolean, runInitialSync: Map<String, Flow<Boolean>>) = koinApplication {
        modules(
            module {
                single<IsNetworkAvailable> {
                    object : IsNetworkAvailable {
                        override fun invoke(): Boolean {
                            return isNetworkAvailable
                        }
                    }
                }
                single<RunInitialSync> {
                    object : RunInitialSync {
                        override fun invoke(accountName: String): Flow<Boolean> {
                            initialSyncWasCalled = true
                            return runInitialSync[accountName] ?: flowOf(false)
                        }
                    }
                }
            }
        )
    }.koin

}