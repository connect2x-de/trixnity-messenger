package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.namedMatrixClients
import de.connect2x.trixnity.messenger.viewmodel.uia.UIA.reactToResponse
import de.connect2x.trixnity.messenger.viewmodel.uia.UIAReaction
import de.connect2x.trixnity.messenger.viewmodel.uia.UIAResponse
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.clientserverapi.model.devices.Device


private val log = KotlinLogging.logger {}

data class DevicesInAccount(
    val thisDevice: DeviceInfo,
    val otherDevices: List<DeviceInfo>,
)

data class AccountWithDevices(
    val accountName: String,
    val devicesInAccount: StateFlow<DevicesInAccount>,
    val isLoading: StateFlow<Boolean>,
    val loadingError: StateFlow<String?>,
)

interface DevicesSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseDevicesSettings: () -> Unit,
    ): DevicesSettingsViewModel {
        return DevicesSettingsViewModelImpl(viewModelContext, onCloseDevicesSettings)
    }

    companion object : DevicesSettingsViewModelFactory
}

interface DevicesSettingsViewModel {
    val accountsWithDevices: StateFlow<List<AccountWithDevices>>
    val error: StateFlow<String?>
    val removeError: StateFlow<String?>
    val showRemoveDevice: MutableStateFlow<String?>
    val showLogin: StateFlow<UIAResponse?>
    val passwordWrong: MutableStateFlow<Boolean>

    fun back()
    fun setDisplayName(accountName: String, deviceId: String, oldDisplayName: String, newDisplayName: String)
    fun verify(accountName: String, deviceId: String)
    fun remove(accountName: String, deviceId: String)
    fun closeRemoveDialog()
    fun authenticate(accountName: String, password: String, deviceId: String)
}

open class DevicesSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseDevicesSettings: () -> Unit,
) : ViewModelContext by viewModelContext, DevicesSettingsViewModel {

    override val accountsWithDevices: StateFlow<List<AccountWithDevices>>
    override val error = MutableStateFlow<String?>(null)
    override val removeError = MutableStateFlow<String?>(null)
    override val showRemoveDevice = MutableStateFlow<String?>(null)
    override val showLogin = MutableStateFlow<UIAResponse?>(null)
    override val passwordWrong = MutableStateFlow(false)

    private val initialLoad = MutableStateFlow(true)

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)
        accountsWithDevices = namedMatrixClients.scopedMapLatest { namedMatrixClients ->
            namedMatrixClients.map { (accountName, matrixClientFlow) ->
                log.trace { "devices for account '$accountName' will be loaded" }
                val isLoading = MutableStateFlow(true)
                val error = MutableStateFlow<String?>(null)
                AccountWithDevices(
                    isLoading = isLoading,
                    loadingError = error,
                    accountName = accountName,
                    devicesInAccount = run {
                        val matrixClient = matrixClientFlow.value
                            ?: throw IllegalStateException("cannot find MatrixClient for account $accountName")
                        log.trace { "get device keys for user ${matrixClient.userId}" }
                        matrixClient.key.getDeviceKeys(matrixClient.userId).map {
                            log.trace { "loading info for devices ${it?.map { it.deviceId }}" }
                            val devices = matrixClient.api.devices.getDevices().getOrNull()
                            log.trace { "devices: $devices" }
                            val thisDevice = devices?.find { it.deviceId == matrixClient.deviceId }?.let {
                                DeviceInfo(
                                    it.deviceId,
                                    MutableStateFlow(displayName(it)),
                                    lastSeenAt(it),
                                    isVerified(
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
                            val otherDevices = devices
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
                                        isVerified(otherDeviceTrustLevel),
                                    )
                                } ?: emptyList()
                            log.trace { "thisDevice: $thisDevice, otherDevices: $otherDevices" }
                            val result = DevicesInAccount(
                                thisDevice = thisDevice,
                                otherDevices = otherDevices,
                            )
                            if (devices == null) {
                                val exc = matrixClient.api.devices.getDevices().exceptionOrNull()
                                log.error(exc) { "Cannot load devices." }
                                error.value = i18n.settingsDevicesLoadError()
                            }

                            log.trace { "device list for account $accountName: $result" }
                            result
                        }.also {
                            log.trace { "mapping of devices to DeviceInfo List finished -> inLoading == false" }
                            isLoading.value = false
                            initialLoad.value = false
                        }
                    }.stateIn(
                        this,
                        SharingStarted.Eagerly, // WhileSubscribed breaks tests
                        DevicesInAccount(
                            thisDevice = DeviceInfo(
                                deviceId = "",
                                displayName = MutableStateFlow(""),
                                lastSeenAt = "",
                                isVerified = MutableStateFlow(false),
                            ),
                            otherDevices = emptyList(),
                        )
                    )
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
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

    override fun setDisplayName(accountName: String, deviceId: String, oldDisplayName: String, newDisplayName: String) {
        val matrixClient = getMatrixClient(accountName)
        if (oldDisplayName != newDisplayName) {
            coroutineScope.launch {
                initialLoad.first { it.not() }
                log.debug { "try to update the device's name ($deviceId, new display name: $newDisplayName)" }
                matrixClient.api.devices.updateDevice(deviceId, newDisplayName).fold(
                    onSuccess = {
                        log.debug { "successfully updated device's display name on the server, now update locally" }
                        accountsWithDevices.value.find { accountWithDevices -> accountWithDevices.accountName == accountName }
                            ?.let { accountWithDevices ->
                                accountWithDevices.devicesInAccount.value.let { accountDevice ->
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
            }
        }
    }

    override fun verify(accountName: String, deviceId: String) {
        val matrixClient = getMatrixClient(accountName)
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

    override fun remove(accountName: String, deviceId: String) {
        val matrixClient = getMatrixClient(accountName)
        coroutineScope.launch {
            initialLoad.first { it.not() }
            val uiaReaction = reactToResponse(matrixClient.api.devices.deleteDevice(deviceId))
            if (uiaReaction is UIAReaction.ShowLogin) {
                showLogin.value = uiaReaction.response
            }
            if (uiaReaction is UIAReaction.UnexpectedError) {
                log.error { "Cannot delete device $deviceId." }
                removeError.value = i18n.settingsDevicesRemoveError()
            }
        }
    }

    override fun closeRemoveDialog() {
        showRemoveDevice.value = null
        passwordWrong.value = false
        removeError.value = null
        showLogin.value = null
    }

    override fun authenticate(accountName: String, password: String, deviceId: String) {
        val matrixClient = getMatrixClient(accountName)
        coroutineScope.launch {
            initialLoad.first { it.not() }
            showLogin.value?.let {
                when (val uiaReaction =
                    it.authenticateWithPassword(matrixClient.userId, password)) {
                    is UIAReaction.ShowLogin -> {
                        passwordWrong.value = true
                        showLogin.value = uiaReaction.response
                        showRemoveDevice.value = deviceId
                    }

                    is UIAReaction.UnexpectedError -> {
                        log.error { "Cannot login: ${uiaReaction.error}." }
                        removeError.value = i18n.settingsDevicesRemoveLoginError(
                            uiaReaction.error ?: ""
                        )
                    }

                    else -> closeRemoveDialog()
                }
            }
        }
    }

    private fun displayName(device: Device?): String {
        return device?.displayName ?: i18n.commonUnknown()
    }

    private fun lastSeenAt(device: Device?): String {
        return device?.lastSeenTs
            ?.let {
                i18n.settingsDevicesDisplayNameLastSeen(Instant.fromEpochMilliseconds(it))
            }
            ?: ""
    }

}

data class DeviceInfo(
    val deviceId: String,
    val displayName: MutableStateFlow<String>,
    val lastSeenAt: String,
    val isVerified: StateFlow<Boolean>
)
