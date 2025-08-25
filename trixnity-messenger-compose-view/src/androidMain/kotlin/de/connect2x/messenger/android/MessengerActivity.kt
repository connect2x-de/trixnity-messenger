package de.connect2x.messenger.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arkivanov.decompose.defaultComponentContext
import de.connect2x.messenger.android.push.disableNotifications
import de.connect2x.messenger.android.push.setPush
import de.connect2x.messenger.compose.view.Client
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.IsFocused
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.compose.view.profiles.Profiles
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.handlePermissionRequest
import de.connect2x.sysnotify.withActivity
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.compose.view.R
import de.connect2x.trixnity.messenger.platformNotifications
import de.connect2x.trixnity.messenger.util.currentImmediateDispatcher
import de.connect2x.trixnity.messenger.util.defaultActivityGetter
import de.connect2x.trixnity.messenger.util.defaultUrlHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessengerActivity : AppCompatActivity() {
    private val log = KotlinLogging.logger { }
    private val matrixMessengerServiceConnection = MatrixMessengerServiceConnection()
    private val scope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, exception ->
        log.error(exception) { "Exception in MessengerActivity coroutine" }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge() // TODO for better UX

        log.debug { "Creating activity instance for '${getString(R.string.app_name)}'" }

        matrixMessengerServiceConnection.bind(applicationContext)
        onNewIntent(intent)

        this.backgroundSyncShouldBeRunning = false

        checkExternalStoragePermissions()

        scope.launch {
            val matrixMultiMessenger = matrixMessengerServiceConnection.matrixMultiMessenger.filterNotNull().first()
            matrixMultiMessenger.defaultActivityGetter { this@MessengerActivity }
            launch {
                matrixMultiMessenger.activeMatrixMessenger.filterNotNull().collectLatest { matrixMessenger ->
                    matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                        .map { it.base.accounts }
                        .distinctUntilChanged()
                        .conflate()
                        .collect { settings ->
                            val anyNotificationsEnabled =
                                settings.any { (_, settings) -> settings.base.notificationsEnabled }
                            withContext(currentImmediateDispatcher()) {
                                if (anyNotificationsEnabled) {
                                    log.debug { "Notifications are enabled for active messenger, requesting permissions" }
                                    matrixMultiMessenger.di.get<NotificationHandler>()
                                        .withActivity { this@MessengerActivity }
                                        .requestPermissions()
                                    scope.launch {
                                        setPush(
                                            applicationContext,
                                            matrixMultiMessenger,
                                            settings.mapValues { it.value.platformNotifications.pushMode },
                                            matrixMessenger,
                                            this,
                                        )
                                    }
                                } else {
                                    disableNotifications(applicationContext)
                                }
                            }
                        }
                }

            }
            withContext(currentImmediateDispatcher()) {
                setContent {
                    WithProfileSelection(
                        matrixMultiMessenger = matrixMultiMessenger,
                        componentContext = defaultComponentContext(),
                        activeMessengerOnce = { _, _ -> },
                        activeMessenger = { matrixMessenger, rootViewModel ->
                            val lifeCycleState =
                                androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.observeAsState()
                            val isFocused = lifeCycleState.value == Lifecycle.Event.ON_RESUME
                            CompositionLocalProvider(
                                Platform provides PlatformType.ANDROID,
                                IsFocused provides isFocused,
                                DI provides matrixMessenger.di,
                            ) {
                                MessengerTheme {
                                    Client(rootViewModel)
                                }
                                LaunchedEffect(Unit) {
                                    val matrixMessengerStartup =
                                        matrixMessenger.di.getOrNull<MatrixMessengerStartup>()
                                    log.debug { "MatrixMessengerStartup: $matrixMessengerStartup" }
                                    if (matrixMessengerStartup != null) {
                                        matrixMessengerStartup(this@MessengerActivity)
                                    } else {
                                        log.info { "no MatrixMessengerStartup implementation found -> ignore" }
                                    }
                                }
                            }
                        }
                    ) { existingProfiles ->
                        val showProfileCreation = remember { mutableStateOf(false) }
                        val lifeCycleState =
                            androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.observeAsState()
                        val isFocused = lifeCycleState.value == Lifecycle.Event.ON_RESUME
                        CompositionLocalProvider(
                            Platform provides PlatformType.ANDROID,
                            IsFocused provides isFocused,
                            DI provides matrixMultiMessenger.di,
                            ShowProfileCreation provides showProfileCreation,
                        ) {
                            MessengerTheme {
                                Profiles(matrixMultiMessenger, existingProfiles)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkExternalStoragePermissions() {
        if (Build.VERSION.SDK_INT < VERSION_CODES.Q) {
            val writeExternalStoragePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (writeExternalStoragePermission == PackageManager.PERMISSION_DENIED) {
                val requestPermissionLauncher =
                    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                        if (isGranted.not()) {
                            // TODO show explanation
                        }
                    }
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        scope.launch {
            matrixMessengerServiceConnection.matrixMultiMessenger.filterNotNull().first().di
                .get<NotificationHandler>()
                .withActivity { this@MessengerActivity }
                .handlePermissionRequest(requestCode, grantResults)
        }
    }

    override fun onStop() {
        log.debug { "Stopping activity" }
        super.onStop()
        this.backgroundSyncShouldBeRunning = true
    }

    override fun onStart() {
        log.debug { "Starting activity" }
        super.onStart()
        this.backgroundSyncShouldBeRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        matrixMessengerServiceConnection.unbind(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when {
            intent.action == Intent.ACTION_VIEW -> intent.data?.also {
                matrixMessengerServiceConnection.matrixMultiMessenger.value?.defaultUrlHandler?.onUri(it)
            }

            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                    val uri = Uri.parse(text)

                    if (uri.scheme == "http" || uri.scheme == "https") {
                        val icon = intent.clipData?.toSequence()?.firstOrNull()?.uri

                        matrixMessengerServiceConnection.onShareData(
                            applicationContext,
                            SharedIntentData.SharedUrl(text, icon)
                        )
                    } else {
                        matrixMessengerServiceConnection.onShareData(
                            applicationContext,
                            SharedIntentData.SharedText(text)
                        )
                    }
                }
            }

            intent.action == Intent.ACTION_SEND ||
                    intent.action == Intent.ACTION_SEND_MULTIPLE -> intent.clipData?.let {
                matrixMessengerServiceConnection.onShareData(
                    applicationContext,
                    intent.clipData?.toList()?.let(SharedIntentData::SharedItems)
                )
            }
        }
    }
}

@Composable
fun Lifecycle.observeAsState(): State<Lifecycle.Event> {
    val state = remember { mutableStateOf(Lifecycle.Event.ON_ANY) }
    DisposableEffect(this) {
        val observer = LifecycleEventObserver { _, event ->
            state.value = event
        }
        this@observeAsState.addObserver(observer)
        onDispose {
            this@observeAsState.removeObserver(observer)
        }
    }
    return state
}
