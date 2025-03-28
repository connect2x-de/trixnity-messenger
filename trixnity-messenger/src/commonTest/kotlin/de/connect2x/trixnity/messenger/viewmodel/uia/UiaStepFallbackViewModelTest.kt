package de.connect2x.trixnity.messenger.viewmodel.uia

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.not
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import net.folivo.trixnity.clientserverapi.model.uia.UIAState
import net.folivo.trixnity.core.ErrorResponse
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class UiaStepFallbackViewModelTest {
    private val onNextMock = mock<(UIA<*>) -> Unit>()
    private val onCancelMock = mock<() -> Unit>()

    init {
        resetMocks(onNextMock, onCancelMock)

        every { onNextMock.invoke(any()) } returns Unit
        every { onCancelMock.invoke() } returns Unit
    }

    @Test
    fun `stop when the UIA is a success`() = runTest {
        val cut = uiaStepFallbackViewModel(AuthenticationType.Password) { _ -> Result.success(UIA.Success(Unit)) }
        cut.confirm()
        eventually(3.seconds) {
            cut.waitForResult.value shouldBe false
            verify { onNextMock.invoke(any()) }
        }
    }

    @Test
    fun `do current stage`() = runTest {
        val cut = uiaStepFallbackViewModel(AuthenticationType.Password) { _ ->
            Result.success(
                UIA.Step(
                    state = UIAState(completed = listOf(AuthenticationType.Recaptcha, AuthenticationType.Password)),
                    getFallbackUrlCallback = { Url("https://localhost") },
                    authenticateCallback = { Result.success(UIA.Success(Unit)) },
                )
            )
        }
        cut.confirm()
        eventually(3.seconds) {
            cut.waitForResult.value shouldBe false
            verify { onNextMock.invoke(any()) }
        }
    }

    @Test
    fun `try to run all stages until completed`() = runTest {
        val cut = uiaStepFallbackViewModel(AuthenticationType.Password) { _ ->
            Result.success(
                UIA.Step(
                    state = UIAState(completed = listOf(AuthenticationType.Recaptcha)),
                    getFallbackUrlCallback = { Url("https://localhost") },
                    authenticateCallback = { Result.success(UIA.Success(Unit)) },
                )
            )
        }
        cut.confirm()
        delay(2_300.milliseconds)
        cut.waitForResult.value shouldBe true
        verify(not) { onNextMock.invoke(any()) }
    }

    @Test
    fun `show an error if UIA authentication failed`() = runTest {
        val cut = uiaStepFallbackViewModel(AuthenticationType.Password) { _ ->
            Result.success(
                UIA.Error(
                    state = UIAState(completed = listOf(AuthenticationType.EmailIdentity)),
                    errorResponse = ErrorResponse.Unauthorized("wrong username/password"),
                    getFallbackUrlCallback = { Url("https://localhost") },
                    authenticateCallback = { Result.success(UIA.Success(Unit)) },
                )
            )
        }
        cut.confirm()
        eventually(3.seconds) {
            cut.waitForResult.value shouldBe true // try for as long as the user needs to complete the request
            cut.error.value shouldContain "wrong username/password"
        }
    }

    @Test
    fun `cancel an ongoing request`() = runTest {
        val cut = uiaStepFallbackViewModel(AuthenticationType.Password) { _ -> Result.success(UIA.Success(Unit)) }
        cut.confirm()
        delay(1.seconds)
        cut.cancel()
        eventually(1.seconds) {
            cut.waitForResult.value shouldBe false
            verify { onCancelMock.invoke() }
        }
    }

    private fun TestScope.uiaStepFallbackViewModel(
        authenticationType: AuthenticationType,
        authenticationCallback: suspend (Any) -> Result<UIA<Unit>>,
    ) = UiaStepFallbackViewModelImpl(
        viewModelContext = testViewModelContext(
            di = koinApplication {
                modules(
                    createTestDefaultTrixnityMessengerModules()
                )
            }.koin,
        ),
        uiaStep = UIA.Step(
            state = UIAState(),
            getFallbackUrlCallback = { Url("https://localhost") },
            authenticateCallback = authenticationCallback,
        ),
        authenticationType = authenticationType,
        onNext = onNextMock,
        onCancel = onCancelMock,
        onError = mock(),
    )
}
