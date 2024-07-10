package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.LoadStoreException.StoreLockedException
import de.connect2x.trixnity.messenger.MatrixClientFactory
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import io.ktor.util.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordLoginViewModelTest : ShouldSpec() {
    val matrixClientFactoryMock = mock<MatrixClientFactory>()

    val matrixClientMock = mock<MatrixClient>()

    private val onBackMock = mock<Function0<Unit>>()
    private val onLoginMock = mock<Function1<MatrixClient, Unit>>()

    init {
        beforeTest {
            Dispatchers.setMain(Dispatchers.Unconfined)

            resetMocks(matrixClientFactoryMock, matrixClientMock, onBackMock, onLoginMock)
            every { onBackMock() } returns Unit
            every { onLoginMock(any()) } returns Unit
            every { matrixClientMock.userId } returns UserId("test", "server")
        }

        should("call login and start sync") {
            everySuspend {
                matrixClientFactoryMock.login(
                    any(),
                    eq(IdentifierType.User("timmy")),
                    eq("sup3rs3cr3t"),
                    any(),
                    any(),
                )
            } returns Result.success(MatrixClientFactory.LoginResult(matrixClientMock, null))
            val cut = viewModel()
            cut.canLogin.first { it }
            cut.tryLogin()

            verifySuspend {
                matrixClientFactoryMock.login(
                    any(),
                    eq(IdentifierType.User("timmy")),
                    eq("sup3rs3cr3t"),
                    any(),
                    any(),
                )
                onLoginMock.invoke(any())
            }
            cut.addMatrixAccountState.value shouldBe AddMatrixAccountState.Success
        }

        should("set addMatrixAccountState when login fails because it was forbidden") {
            everySuspend {
                matrixClientFactoryMock.login(
                    any(),
                    eq(IdentifierType.User("timmy")),
                    eq("sup3rs3cr3t"),
                    any(),
                    any(),
                )
            } returns Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("403")))
            val cut = viewModel()
            cut.canLogin.first { it }
            cut.tryLogin()

            cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "not correct"
        }


        should("show the correct error message when server is configured wrong") {
            everySuspend {
                matrixClientFactoryMock.login(
                    any(),
                    eq(IdentifierType.User("timmy")),
                    eq("sup3rs3cr3t"),
                    any(),
                    any(),
                )
            } returns Result.failure(UnresolvedAddressException())
            val cut = viewModel()
            cut.tryLogin()

            cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "address"
        }

        should("show the correct error message when an unknown error occurs") {
            everySuspend {
                matrixClientFactoryMock.login(
                    any(),
                    eq(IdentifierType.User("timmy")),
                    eq("sup3rs3cr3t"),
                    any(),
                    any(),
                )
            } returns Result.failure(Exception("Something unexpected."))
            val cut = viewModel()
            cut.canLogin.first { it }
            cut.tryLogin()

            cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "Matrix server"
        }

        should("cancel login when user aborts the login") {
            everySuspend {
                matrixClientFactoryMock.login(
                    any(),
                    eq(IdentifierType.User("timmy")),
                    eq("sup3rs3cr3t"),
                    any(),
                    any(),
                )
            } returns Result.success(MatrixClientFactory.LoginResult(matrixClientMock, null))
            val cut = viewModel()
            cut.canLogin.first { it }
            cut.back()

            verify {
                onBackMock.invoke()
            }
        }

        should("abort with correct Exception in callback when store is locked") {
            everySuspend {
                matrixClientFactoryMock.login(
                    any(),
                    eq(IdentifierType.User("timmy")),
                    eq("sup3rs3cr3t"),
                    any(),
                    any(),
                )
            } calls { throw StoreLockedException() }
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
