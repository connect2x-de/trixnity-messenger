package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.LoadStoreException.StoreLockedException
import de.connect2x.trixnity.messenger.MatrixClientFactory
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.invoke
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction1
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordLoginViewModelTest : ShouldSpec() {
    val mocker = Mocker()

    @Mock
    lateinit var matrixClientFactoryMock: MatrixClientFactory

    @Mock
    lateinit var matrixClientMock: MatrixClient

    private val onBackMock = mockFunction0<Unit>(mocker)
    private val onLoginMock = mockFunction1<Unit, MatrixClient>(mocker)

    init {
        beforeTest {
            mocker.reset()
            Dispatchers.setMain(Dispatchers.Unconfined)
            injectMocks(mocker)

            with(mocker) {
                every { onBackMock() } returns Unit
                every { onLoginMock(isAny()) } returns Unit
                every { matrixClientMock.userId } returns UserId("test", "server")
            }
        }

        should("call login and start sync") {
            mocker.everySuspending {
                matrixClientFactoryMock.login(
                    isAny(),
                    isEqual(IdentifierType.User("timmy")),
                    isEqual("sup3rs3cr3t"),
                    isAny(),
                    isAny(),
                )
            } returns Result.success(MatrixClientFactory.LoginResult(matrixClientMock, null))
            val cut = viewModel()
            cut.canLogin.first { it }
            cut.tryLogin()

            mocker.verifyWithSuspend(exhaustive = false) {
                matrixClientFactoryMock.login(
                    isAny(),
                    isEqual(IdentifierType.User("timmy")),
                    isEqual("sup3rs3cr3t"),
                    isAny(),
                    isAny(),
                )
                onLoginMock.invoke(isAny())
            }
            cut.addMatrixAccountState.value shouldBe AddMatrixAccountState.Success
        }

        should("set addMatrixAccountState when login fails because it was forbidden") {
            mocker.everySuspending {
                matrixClientFactoryMock.login(
                    isAny(),
                    isEqual(IdentifierType.User("timmy")),
                    isEqual("sup3rs3cr3t"),
                    isAny(),
                    isAny(),
                )
            } returns Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("403")))
            val cut = viewModel()
            cut.canLogin.first { it }
            cut.tryLogin()

            cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "not correct"
        }


        should("show the correct error message when server is configured wrong") {
            mocker.everySuspending {
                matrixClientFactoryMock.login(
                    isAny(),
                    isEqual(IdentifierType.User("timmy")),
                    isEqual("sup3rs3cr3t"),
                    isAny(),
                    isAny(),
                )
            } returns Result.failure(UnresolvedAddressException())
            val cut = viewModel()
            cut.tryLogin()

            cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "address"
        }

        should("show the correct error message when an unknown error occurs") {
            mocker.everySuspending {
                matrixClientFactoryMock.login(
                    isAny(),
                    isEqual(IdentifierType.User("timmy")),
                    isEqual("sup3rs3cr3t"),
                    isAny(),
                    isAny(),
                )
            } returns Result.failure(Exception("Something unexpected."))
            val cut = viewModel()
            cut.canLogin.first { it }
            cut.tryLogin()

            cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "Matrix server"
        }

        should("cancel login when user aborts the login") {
            mocker.everySuspending {
                matrixClientFactoryMock.login(
                    isAny(),
                    isEqual(IdentifierType.User("timmy")),
                    isEqual("sup3rs3cr3t"),
                    isAny(),
                    isAny(),
                )
            } returns Result.success(MatrixClientFactory.LoginResult(matrixClientMock, null))
            val cut = viewModel()
            cut.canLogin.first { it }
            cut.back()

            mocker.verify(exhaustive = false) {
                onBackMock.invoke()
            }
        }

        should("abort with correct Exception in callback when store is locked") {
            mocker.everySuspending {
                matrixClientFactoryMock.login(
                    isAny(),
                    isEqual(IdentifierType.User("timmy")),
                    isEqual("sup3rs3cr3t"),
                    isAny(),
                    isAny(),
                )
            } runs { throw StoreLockedException() }
            val cut = viewModel()
            cut.canLogin.first { it }
            cut.tryLogin()

            cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "instance"
        }
    }

    private suspend fun viewModel(serverUrl: String = "http://localhost"): PasswordLoginViewModelImpl {
        val di = koinApplication {
            modules(createTestDefaultTrixnityMessengerModules() + module {
                single<MatrixClientFactory> { matrixClientFactoryMock }
            })
        }.koin
        di.get<I18n>().setCurrentLang(DefaultLanguages.EN)
        return PasswordLoginViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                di,
                DefaultComponentContext(LifecycleRegistry()),
                coroutineContext = Dispatchers.Unconfined,
            ),
            serverUrl = serverUrl,
            onLogin = onLoginMock,
            onBack = onBackMock,
        ).apply {
            username.value = "timmy"
            password.value = "sup3rs3cr3t"
        }
    }
}
