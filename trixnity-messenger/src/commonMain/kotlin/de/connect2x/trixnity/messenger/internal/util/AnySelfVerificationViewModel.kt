package de.connect2x.trixnity.messenger.internal.util

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlin.jvm.JvmInline

@TrixnityMessengerPrivateApi
sealed interface AnySelfVerificationViewModel {

    @TrixnityMessengerPrivateApi
    @JvmInline
    value class V1(
        @Suppress("DEPRECATION")
        val value: de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
    ) : AnySelfVerificationViewModel

    @TrixnityMessengerPrivateApi
    @JvmInline
    value class V2(val value: de.connect2x.trixnity.messenger.viewmodel.verification.v2.SelfVerificationViewModel) :
        AnySelfVerificationViewModel
}
