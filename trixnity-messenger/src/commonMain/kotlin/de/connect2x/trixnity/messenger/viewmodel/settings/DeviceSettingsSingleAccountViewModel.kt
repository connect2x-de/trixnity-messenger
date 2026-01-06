package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.lifecycle.doOnStart
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.clientserverapi.model.authentication.oauth2.OAuth2AccountManagementAction
import net.folivo.trixnity.clientserverapi.model.devices.Device
import net.folivo.trixnity.core.MSC3814
import net.folivo.trixnity.core.MSC4191
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.crypto.key.DeviceTrustLevel
import org.koin.core.component.get
import kotlin.time.Instant


private val log = KotlinLogging.logger {}

interface DeviceSettingsSingleAccountViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
    ): DeviceSettingsSingleAccountViewModel =
        DeviceSettingsSingleAccountViewModelImpl(
            viewModelContext,
        )

    companion object : DeviceSettingsSingleAccountViewModelFactory
}

interface DeviceSettingsSingleAccountViewModel {
    val account: UserId

    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>

    val devices: StateFlow<List<DeviceInfo>?>

    data class DeviceInfo(
        val isThisDevice: Boolean,
        val deviceId: String,
        val displayName: String,
        val lastSeenAt: String,
        val isVerified: Boolean,
        @MSC3814
        val isDehydrated: Boolean,
    )

    fun setDisplayName(deviceId: String, displayName: String)
    fun verify(deviceId: String)
    fun update()
    fun remove(deviceId: String)
}

open class DeviceSettingsSingleAccountViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : DeviceSettingsSingleAccountViewModel, MatrixClientViewModelContext by viewModelContext {

    override val account: UserId = userId
    private val authorizeUia = get<AuthorizeUia>()
    private val uriCaller = get<UriCaller>()
    private val triggerDeviceListUpdate = MutableSharedFlow<Unit>(replay = 1)

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error

    @OptIn(ExperimentalCoroutinesApi::class)
    override val devices =
        combine(
            triggerDeviceListUpdate,
            matrixClient.key.getDeviceKeys(userId)
        ) { _, deviceKeys ->
            deviceKeys
        }.flatMapLatest { deviceKeys ->
            log.trace { "devices for account $userId will be loaded" }
            _isLoading.value = true
            _error.value = null
            log.trace { "get device keys for user $userId" }
            val devices = matrixClient.api.device.getDevices()
                .onFailure {
                    log.warn(it) { "Cannot load devices from server." }
                    _error.value = i18n.settingsDevicesLoadError()
                }
                .getOrNull()
            _isLoading.value = false
            log.trace { "devices: $devices" }
            if (devices.isNullOrEmpty()) flowOf(emptyList())
            else combine(devices.map { device ->
                matrixClient.key.getTrustLevel(userId, device.deviceId)
                    .map { device to (it is DeviceTrustLevel.CrossSigned && it.verified) }
            }) { deviceWithVerified ->
                deviceWithVerified
                    .sortedByDescending { (device, _) ->
                        device.lastSeenTs
                    }.map { (device, isVerified) ->
                        DeviceSettingsSingleAccountViewModel.DeviceInfo(
                            isThisDevice = device.deviceId == matrixClient.deviceId,
                            deviceId = device.deviceId,
                            displayName = displayName(device),
                            lastSeenAt = lastSeenAt(device),
                            isVerified = isVerified,
                            isDehydrated =
                                @OptIn(MSC3814::class)
                                deviceKeys?.find { it.deviceId == device.deviceId }?.dehydrated == true,
                        )
                    }
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    init {
        lifecycle.doOnStart { update() }
    }

    private fun displayName(device: Device): String =
        device.displayName ?: i18n.unknownDevice()

    private fun lastSeenAt(device: Device?): String = device?.lastSeenTs
        ?.let { i18n.settingsDevicesDisplayNameLastSeen(Instant.fromEpochMilliseconds(it)) } ?: ""

    override fun setDisplayName(
        deviceId: String,
        displayName: String,
    ) {
        val matrixClient = getMatrixClient(userId)
        coroutineScope.launch {
            _error.value = null
            log.debug { "try to update the device's name ($deviceId, new display name: $displayName)" }
            matrixClient.api.device.updateDevice(deviceId, displayName).fold(
                onSuccess = {
                    log.debug { "successfully updated device's display name on the server, now update locally" }
                },
                onFailure = {
                    log.error(it) { "Cannot change device display name for $deviceId." }
                    _error.value = i18n.settingsDevicesDisplayNameError()
                }
            )
            update()
        }
    }

    override fun verify(deviceId: String) {
        coroutineScope.launch {
            _error.value = null
            try {
                matrixClient.verification.createDeviceVerificationRequest(
                    matrixClient.userId,
                    setOf(deviceId)
                ).getOrThrow()
            } catch (exc: Exception) {
                log.error(exc) { "Cannot create a device verification for $deviceId." }
                _error.value = i18n.settingsDevicesVerificationError()
            }
        }
    }

    override fun update() {
        coroutineScope.launch {
            triggerDeviceListUpdate.emit(Unit)
        }
    }

    override fun remove(deviceId: String) {
        val matrixClient = getMatrixClient(userId)
        coroutineScope.launch {
            _error.value = null
            val authServerMetadata = matrixClient.serverData.value?.auth
            @OptIn(MSC4191::class)
            if (authServerMetadata != null) {
                val accountManagementUri = authServerMetadata.accountManagementUri
                val accountManagementActionsSupported = authServerMetadata.accountManagementActionsSupported
                if (accountManagementUri == null || accountManagementActionsSupported == null ||
                    accountManagementActionsSupported.contains(OAuth2AccountManagementAction.EndSession).not()
                ) {
                    log.warn { "account management uri or actions are not supported by the auth server" }
                    _error.value = i18n.settingsDevicesRemoveError()
                } else {
                    uriCaller(URLBuilder(accountManagementUri).apply {
                        parameters.append("action", OAuth2AccountManagementAction.EndSession.value)
                        parameters.append("device_id", deviceId)
                    }.build().toString(), true)
                }
            } else {
                val displayName = matrixClient.api.device.getDevice(deviceId).fold(
                    onSuccess = { it.displayName },
                    onFailure = { deviceId },
                )
                val result = authorizeUia(
                    i18n.settingsDevicesRemoveConfirmationMessage(displayName, deviceId)
                ) {
                    matrixClient.api.device.deleteDevice(deviceId)
                }
                when (result) {
                    is AuthorizeUiaResult.CancelledByUser ->
                        _error.value = result.message

                    is AuthorizeUiaResult.Error ->
                        _error.value = i18n.settingsDevicesRemoveError(result.exception.errorResponse.error)

                    is AuthorizeUiaResult.UnexpectedError ->
                        _error.value = result.message

                    is AuthorizeUiaResult.Success -> {
                        log.debug { "successfully removed device $deviceId for user $userId" }
                        update()
                    }
                }
            }
        }
    }
}
