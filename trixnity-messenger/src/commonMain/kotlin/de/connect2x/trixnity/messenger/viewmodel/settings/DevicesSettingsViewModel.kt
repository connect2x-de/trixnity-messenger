package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.clientserverapi.model.devices.Device
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.crypto.key.DeviceTrustLevel
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

data class DevicesInAccount(
    val thisDevice: DeviceInfo,
    val otherDevices: List<DeviceInfo>,
)

data class AccountWithDevices(
    val userId: UserId,
    val devicesInAccount: StateFlow<DevicesInAccount?>,
    val isLoading: StateFlow<Boolean>,
    val loadingError: StateFlow<String?>,
)

interface DevicesSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseDevicesSettings: () -> Unit,
    ): DevicesSettingsViewModel = DevicesSettingsViewModelImpl(
        viewModelContext,
        onCloseDevicesSettings,
    )

    companion object : DevicesSettingsViewModelFactory
}

interface DevicesSettingsViewModel {
    val accountsWithDevices: StateFlow<List<AccountWithDevices>>
    val error: StateFlow<String?>
    val removalForDeviceId: MutableStateFlow<String?>

    fun back()
    fun setDisplayName(userId: UserId, deviceId: String, oldDisplayName: String, newDisplayName: String)
    fun verify(userId: UserId, deviceId: String)
    fun updateDeviceList()
    fun remove(userId: UserId, deviceId: String)
}

open class DevicesSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseDevicesSettings: () -> Unit,
) : ViewModelContext by viewModelContext, DevicesSettingsViewModel {
    private val triggerDeviceListUpdate = MutableSharedFlow<Unit>(replay = 1)
    override val accountsWithDevices: StateFlow<List<AccountWithDevices>> =
        combine(matrixClients, triggerDeviceListUpdate) { m, _ -> m }
            .scopedMapLatest { matrixClients ->
                matrixClients.map { (userId, matrixClient) ->
                    log.trace { "devices for account '$userId' will be loaded" }
                    val isLoading = MutableStateFlow(false)
                    val error = MutableStateFlow<String?>(null)
                    AccountWithDevices(
                        isLoading = isLoading,
                        loadingError = error,
                        userId = userId,
                        devicesInAccount = run {
                            log.trace { "get device keys for user ${matrixClient.userId}" }
                            matrixClient.key.getDeviceKeys(matrixClient.userId).map {
                                error.value = null
                                isLoading.value = true
                                log.trace { "loading info for devices ${it?.map { it.deviceId }}" }
                                val devices = matrixClient.api.device.getDevices()
                                log.trace { "devices: $devices" }
                                val thisDevice =
                                    devices.getOrNull()?.find { it.deviceId == matrixClient.deviceId }?.let {
                                        DeviceInfo(
                                            it.deviceId,
                                            MutableStateFlow(displayName(it)),
                                            lastSeenAt(it),
                                            coroutineScope.isVerified(
                                                matrixClient.key.getTrustLevel(
                                                    matrixClient.userId,
                                                    matrixClient.deviceId,
                                                )
                                            ),
                                        )
                                    } ?: DeviceInfo(
                                        deviceId = "",
                                        displayName = MutableStateFlow(""),
                                        lastSeenAt = "",
                                        isVerified = MutableStateFlow(false),
                                    )
                                val otherDevices = devices.getOrNull()
                                    ?.filterNot { it.deviceId == matrixClient.deviceId }
                                    ?.sortedByDescending { it.lastSeenTs }
                                    ?.map {
                                        val otherDeviceTrustLevel =
                                            matrixClient.key.getTrustLevel(
                                                matrixClient.userId,
                                                it.deviceId,
                                            )
                                        DeviceInfo(
                                            it.deviceId,
                                            MutableStateFlow(displayName(it)),
                                            lastSeenAt(it),
                                            coroutineScope.isVerified(otherDeviceTrustLevel),
                                        )
                                    } ?: emptyList()
                                log.trace { "thisDevice: $thisDevice, otherDevices: $otherDevices" }
                                val result = DevicesInAccount(
                                    thisDevice = thisDevice,
                                    otherDevices = otherDevices,
                                )
                                if (devices.isFailure) {
                                    val exc = devices.exceptionOrNull()
                                    log.warn(exc) { "Cannot load devices from server." }
                                    error.value = i18n.settingsDevicesLoadError()
                                }

                                log.trace { "device list for account $userId: $result" }
                                result
                                    .also {
                                        log.trace { "mapping of devices to DeviceInfo List finished -> inLoading == false" }
                                        isLoading.value = false
                                        initialLoad.value = false
                                    }
                            }
                        }.stateIn(
                            this,
                            SharingStarted.WhileSubscribed(),
                            null,
                        )
                    )
                }
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    override val error = MutableStateFlow<String?>(null)

    private val initialLoad = MutableStateFlow(true)
    override val removalForDeviceId = MutableStateFlow<String?>(null)
    private val authorizeUia = get<AuthorizeUia>()

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)

        coroutineScope.launch {
            triggerDeviceListUpdate.emit(Unit)
        }
    }

    private fun CoroutineScope.isVerified(
        trustLevel: Flow<DeviceTrustLevel>,
    ): StateFlow<Boolean> =
        trustLevel.map {
            it.isVerified
        }.stateIn(this, SharingStarted.Eagerly, false)

    override fun back() {
        onCloseDevicesSettings()
    }

    override fun setDisplayName(
        userId: UserId,
        deviceId: String,
        oldDisplayName: String,
        newDisplayName: String,
    ) {
        val matrixClient = getMatrixClient(userId)
        if (oldDisplayName != newDisplayName) {
            coroutineScope.launch {
                initialLoad.first { it.not() }
                log.debug { "try to update the device's name ($deviceId, new display name: $newDisplayName)" }
                matrixClient.api.device.updateDevice(deviceId, newDisplayName).fold(
                    onSuccess = {
                        log.debug { "successfully updated device's display name on the server, now update locally" }
                        accountsWithDevices.value.find { accountWithDevices -> accountWithDevices.userId == userId }
                            ?.let { accountWithDevices ->
                                accountWithDevices.devicesInAccount.value?.let { accountDevice ->
                                    if (accountDevice.thisDevice.deviceId == deviceId)
                                        accountDevice.thisDevice
                                    else {
                                        accountDevice.otherDevices.find { it.deviceId == deviceId }
                                    }
                                }?.let {
                                    it.displayName.value = newDisplayName
                                    log.info { "changed display name to '$newDisplayName'" }
                                }
                            }
                    },
                    onFailure = {
                        log.error(it) { "Cannot change device display name for $deviceId." }
                        error.value = i18n.settingsDevicesDisplayNameError()
                    }
                )
                updateDeviceList()
            }
        }
    }

    override fun verify(userId: UserId, deviceId: String) {
        val matrixClient = getMatrixClient(userId)
        coroutineScope.launch {
            initialLoad.first { it.not() }
            try {
                matrixClient.verification.createDeviceVerificationRequest(
                    matrixClient.userId,
                    setOf(deviceId)
                ).getOrThrow()
            } catch (exc: Exception) {
                log.error(exc) { "Cannot create a device verification for $deviceId." }
                error.value = i18n.settingsDevicesVerificationError()
            }
        }
    }

    override fun updateDeviceList() {
        coroutineScope.launch {
            triggerDeviceListUpdate.emit(Unit)
        }
    }


    override fun remove(userId: UserId, deviceId: String) {
        val matrixClient = getMatrixClient(userId)
        coroutineScope.launch {
            initialLoad.first { it.not() }
            removalForDeviceId.value = deviceId
            error.value = null
            val displayName = matrixClient.api.device.getDevice(deviceId, userId).fold(
                onSuccess = { it.displayName },
                onFailure = { null },
            )
            val result = authorizeUia(
                i18n.settingsDevicesRemoveConfirmationMessage(displayName, deviceId)
            ) {
                getMatrixClient(userId).api.device.deleteDevice(deviceId)
            }
            when (result) {
                is AuthorizeUiaResult.CancelledByUser -> error.value = result.message

                is AuthorizeUiaResult.Error ->
                    error.value = i18n.settingsDevicesRemoveError(result.exception.errorResponse.error)

                is AuthorizeUiaResult.UnexpectedError -> error.value = result.message

                is AuthorizeUiaResult.Success ->
                    log.debug { "successfully removed device $deviceId for user $userId" }
            }
            removalForDeviceId.value = null
            updateDeviceList()
        }
    }

    private fun displayName(device: Device?): String =
        device?.displayName ?: i18n.commonUnknown()

    private fun lastSeenAt(device: Device?): String = device?.lastSeenTs
        ?.let {
            i18n.settingsDevicesDisplayNameLastSeen(Instant.fromEpochMilliseconds(it))
        } ?: ""
}

data class DeviceInfo(
    val deviceId: String,
    val displayName: MutableStateFlow<String>,
    val lastSeenAt: String,
    val isVerified: StateFlow<Boolean>
)
