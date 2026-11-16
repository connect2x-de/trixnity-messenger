package de.connect2x.trixnity.messenger.call

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.TimeZone

@OptIn(TrixnityMessengerCallApi::class)
internal fun TestScope.testCall(
    target: CallTarget,
    signalling: CallSignalling = TestCallSignalling(),
    media: TestCallMedia = TestCallMedia(),
    matrixClients: MatrixClients = testCallClients(MutableStateFlow(mapOf(target.userId to mock<MatrixClient>()))),
    scope: CoroutineScope = backgroundScope,
    mediaFactory: CallMediaFactory = CallMediaFactory { scope, _, _ ->
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                media.closes++
            }
        }
        media
    },
): CallSession {
    return CallSessionImpl(
        parentScope = scope,
        target = target,
        mediaFactory = mediaFactory,
        createSignalling = { _, _ -> signalling },
        matrixClients = matrixClients,
        i18n = I18n(DefaultLanguages, createTestMatrixMessengerSettingsHolder(), GetSystemLang { "en" }, TimeZone.UTC),
    )
}

internal fun testCallClients(clients: StateFlow<Map<UserId, MatrixClient>>): MatrixClients {
    val registry = mock<MatrixClients>()
    every { registry.value } calls { clients.value }
    everySuspend { registry.collect(any()) } calls { clients.collect(it.arg(0)) }
    return registry
}

@OptIn(TrixnityMessengerCallApi::class, MSC4143::class)
internal class TestCallSignalling : CallSignalling {
    var closes = 0
    override val roomInfo = MutableStateFlow<RoomInfoElement?>(null)
    override val publishing = MutableStateFlow<CallSignalling.LiveKitCallParameters?>(null)
    override val receiving = MutableStateFlow(emptyList<CallSignalling.LiveKitCallParameters>())
    override val members = MutableStateFlow(emptyMap<Pair<UserId, RtcMemberId>, UserInfoElement>())
    override val error = MutableStateFlow<String?>(null)

    override fun close() {
        closes++
    }
}

@OptIn(TrixnityMessengerCallApi::class)
internal class TestCallMedia(private val onPermissionResult: (CallPermissionKind, Boolean) -> Unit = { _, _ -> }) :
    CallMedia {
    var microphoneToggles = 0
    var cameraToggles = 0
    var closes = 0
    override val state = MutableStateFlow(CallMediaState())

    override fun onPermissionRequestResult(kind: CallPermissionKind, granted: Boolean) {
        onPermissionResult(kind, granted)
    }

    override fun toggleMicrophone() {
        microphoneToggles++
    }

    override fun toggleCamera() {
        cameraToggles++
    }
}
