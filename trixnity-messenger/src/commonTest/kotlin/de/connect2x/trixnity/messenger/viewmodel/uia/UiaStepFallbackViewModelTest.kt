package de.connect2x.trixnity.messenger.viewmodel.uia

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.not
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.*
import kotlinx.coroutines.delay
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import net.folivo.trixnity.clientserverapi.model.uia.UIAState
import net.folivo.trixnity.core.ErrorResponse
import org.koin.dsl.koinApplication
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class UiaStepFallbackViewModelTest : ShouldSpec() {
    override fun timeout() = 4_000L

    private val onNextMock = mock<(UIA<*>) -> Unit>()
    private val onCancelMock = mock<() -> Unit>()

    init {
        beforeTest {
            resetMocks(onNextMock, onCancelMock)

            every { onNextMock.invoke(any()) } returns Unit
            every { onCancelMock.invoke() } returns Unit
        }

        should("stop when the UIA is a success") {
            val cut = uiaStepFallbackViewModel(AuthenticationType.Password) { _ -> Result.success(UIA.Success(Unit)) }
            cut.confirm()
            eventually(3.seconds) {
                cut.waitForResult.value shouldBe false
                verify { onNextMock.invoke(any()) }
            }
        }

        should("do current stage") {
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

        should("try to run all stages until completed") {
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

        should("not support e-mail or phone verification (might be a feature in the future)") {
            val cut =
                uiaStepFallbackViewModel(AuthenticationType.EmailIdentity) { _ -> Result.success(UIA.Success(Unit)) }
            cut.error.value shouldContain "not supported"
            cut.confirm()
            continually(3.seconds) {
                cut.waitForResult.value shouldBe false
                cut.error.value shouldContain "not supported"
            }
        }

        should("show an error if UIA authentication failed") {
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

        should("cancel an ongoing request") {
            val cut = uiaStepFallbackViewModel(AuthenticationType.Password) { _ -> Result.success(UIA.Success(Unit)) }
            cut.confirm()
            delay(1.seconds)
            cut.cancel()
            eventually(1.seconds) {
                cut.waitForResult.value shouldBe false
                verify { onCancelMock.invoke() }
            }
        }
    }

    private fun uiaStepFallbackViewModel(
        authenticationType: AuthenticationType,
        authenticationCallback: suspend (Any) -> Result<UIA<Unit>>,
    ) = UiaStepFallbackViewModelImpl(
        viewModelContext = ViewModelContextImpl(
            di = koinApplication {
                modules(
                    createTestDefaultTrixnityMessengerModules()
                )
            }.koin,
            componentContext = DefaultComponentContext(LifecycleRegistry()),
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
