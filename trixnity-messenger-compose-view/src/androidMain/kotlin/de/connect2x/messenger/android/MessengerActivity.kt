package de.connect2x.messenger.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arkivanov.decompose.defaultComponentContext
import de.connect2x.trixnity_messenger_compose_view.generated.resources.Res
import de.connect2x.messenger.android.push.setPush
import de.connect2x.messenger.compose.view.Client
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.GetLicences
import de.connect2x.messenger.compose.view.GetLicencesImpl
import de.connect2x.messenger.compose.view.ImeVisible
import de.connect2x.messenger.compose.view.IsDebug
import de.connect2x.messenger.compose.view.IsFocused
import de.connect2x.messenger.compose.view.LocalWindowScope
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.compose.view.R
import de.connect2x.messenger.compose.view.profiles.Profiles
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.platformNotifications
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
import org.jetbrains.compose.resources.ExperimentalResourceApi


private val log = KotlinLogging.logger { }

private const val REQUEST_POST_NOTIFICATIONS = 123456789

@OptIn(ExperimentalResourceApi::class)
class MessengerActivity : AppCompatActivity() {
    private val matrixMessengerServiceConnection = MatrixMessengerServiceConnection()
    private val scope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { coroutineContext, throwable ->
        log.error(throwable) { "error in main scope" }
    })


    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log.debug { "... Activity: onCreate()" }
        log.debug { "App name: ${getString(R.string.app_name)}" }

        matrixMessengerServiceConnection.bind(applicationContext)

        this.backgroundSyncShouldBeRunning = false

        NotificationManagerCompat.from(this).cancelAll()

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

        scope.launch {
            val matrixMultiMessenger = matrixMessengerServiceConnection.matrixMultiMessenger.filterNotNull().first()
            launch {
                matrixMultiMessenger.activeMatrixMessenger.filterNotNull().collectLatest { matrixMessenger ->
                    matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                        .map { settings ->
                            settings.base.accounts.map { it.key to it.value }.toMap()
                        }
                        .distinctUntilChanged()
                        .conflate()
                        .collect { settings ->
                            val anyNotificationsEnabled =
                                settings.any { (_, settings) -> settings.base.notificationsEnabled }
                            withContext(Dispatchers.Main) {
                                if (anyNotificationsEnabled) {
                                    checkPermissionForNotifications()
                                    setPush(
                                        applicationContext,
                                        settings.mapValues { it.value.platformNotifications.pushMode },
                                        matrixMessenger,
                                    )
                                }
                            }
                        }
                }
            }
            withContext(Dispatchers.Main) {
                setContent {
                    // decorFitsSystemWindows == true seems to speed up the animation of the IME
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    WithProfileSelection(
                        matrixMultiMessenger = matrixMultiMessenger,
                        componentContext = defaultComponentContext(),
                        activeMessengerOnce = { _, _ -> },
                        activeMessenger = { matrixMessenger, rootViewModel ->
                            Surface {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .navigationBarsPadding()
                                        .statusBarsPadding()
                                        .padding(bottom = with(LocalDensity.current) {
                                            (WindowInsets.ime.getBottom(this) - WindowInsets.navigationBars.getBottom(
                                                this
                                            ))
                                                .coerceAtLeast(0)
                                                .toDp()
                                        })
                                ) {
                                    val lifeCycleState = LocalLifecycleOwner.current.lifecycle.observeAsSate()
                                    val isFocused = lifeCycleState.value == Lifecycle.Event.ON_RESUME
                                    CompositionLocalProvider(
                                        ImeVisible provides WindowInsets.isImeVisible,
                                        Platform provides PlatformType.ANDROID,
                                        IsFocused provides isFocused,
                                        LocalWindowScope provides null,
                                        IsDebug provides false,
                                        DI provides matrixMessenger.di,
                                        GetLicences provides GetLicencesImpl {
                                            Res.readBytes("files/aboutlibraries.json").decodeToString()
                                        },
                                    ) {
                                        MessengerTheme {
                                            Client(rootViewModel)
                                        }
                                    }
                                }
                            }
                        }
                    ) { existingProfiles ->
                        val showProfileCreation = remember { mutableStateOf(false) }
                        val lifeCycleState = LocalLifecycleOwner.current.lifecycle.observeAsSate()
                        val isFocused = lifeCycleState.value == Lifecycle.Event.ON_RESUME
                        CompositionLocalProvider(
                            ImeVisible provides WindowInsets.isImeVisible,
                            Platform provides PlatformType.ANDROID,
                            IsFocused provides isFocused,
                            LocalWindowScope provides null,
                            IsDebug provides false,
                            DI provides matrixMultiMessenger.di,
                            ShowProfileCreation provides showProfileCreation,
                        ) {
                            MessengerTheme {
                                Profiles(matrixMultiMessenger, existingProfiles, onCancel = {
                                    finishAndRemoveTask()
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionForNotifications() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED -> {
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_POST_NOTIFICATIONS,
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_POST_NOTIFICATIONS -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED).not()
                ) {
                    // TODO explanation
                }
                return
            }
        }
    }

    override fun onStop() {
        super.onStop()
        log.debug { "onStop" }
        this.backgroundSyncShouldBeRunning = true
    }

    override fun onStart() {
        super.onStart()
        log.debug { "onStart" }
        this.backgroundSyncShouldBeRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        matrixMessengerServiceConnection.unbind(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.also {
            matrixMessengerServiceConnection.matrixMultiMessenger.value?.defaultUrlHandler?.onUri(it)
        }
    }
}

@Composable
fun Lifecycle.observeAsSate(): State<Lifecycle.Event> {
    val state = remember { mutableStateOf(Lifecycle.Event.ON_ANY) }
    DisposableEffect(this) {
        val observer = LifecycleEventObserver { _, event ->
            state.value = event
        }
        this@observeAsSate.addObserver(observer)
        onDispose {
            this@observeAsSate.removeObserver(observer)
        }
    }
    return state
}
