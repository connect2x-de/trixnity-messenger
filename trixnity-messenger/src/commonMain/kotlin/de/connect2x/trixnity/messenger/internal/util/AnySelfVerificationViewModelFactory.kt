package de.connect2x.trixnity.messenger.internal.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlin.jvm.JvmInline

internal sealed interface AnySelfVerificationViewModelFactory {
    @JvmInline
    value class V1(
        @Suppress("DEPRECATION")
        val value: de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModelFactory
    ) : AnySelfVerificationViewModelFactory

    @JvmInline
    value class V2(
        val value: de.connect2x.trixnity.messenger.viewmodel.verification.v2.SelfVerificationViewModelFactory
    ) : AnySelfVerificationViewModelFactory

    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onCloseSelfVerification: (completedVerification: Boolean) -> Unit,
        onResetRecovery: () -> Unit,
    ): AnySelfVerificationViewModel {
        return when (this) {
            is V1 ->
                value
                    .create(
                        viewModelContext = viewModelContext,
                        onCloseSelfVerification = onCloseSelfVerification,
                        onResetRecovery = onResetRecovery,
                    )
                    .let(AnySelfVerificationViewModel::V1)
            is V2 ->
                value
                    .create(
                        viewModelContext = viewModelContext,
                        onCloseSelfVerification = { onCloseSelfVerification(true) },
                        onResetRecovery = onResetRecovery,
                    )
                    .let(AnySelfVerificationViewModel::V2)
        }
    }
}

internal fun AnySelfVerificationViewModelFactory(
    matrixMessengerConfiguration: MatrixMessengerConfiguration,
    selfVerificationV1ViewModelFactory:
        @Suppress("DEPRECATION")
        de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModelFactory,
    selfVerificationV2ViewModelFactory:
        de.connect2x.trixnity.messenger.viewmodel.verification.v2.SelfVerificationViewModelFactory,
): AnySelfVerificationViewModelFactory {
    return if (matrixMessengerConfiguration.features.enableNewAccountWizard) {
        AnySelfVerificationViewModelFactory.V2(value = selfVerificationV2ViewModelFactory)
    } else {
        AnySelfVerificationViewModelFactory.V1(value = selfVerificationV1ViewModelFactory)
    }
}
