package de.connect2x.trixnity.messenger.compose.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import com.arkivanov.decompose.defaultComponentContext
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.compose.view.profiles.IntroductionOrProfile
import de.connect2x.trixnity.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.trixnity.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.trixnity.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.trixnity.messenger.compose.view.theme.MessengerTheme
import de.connect2x.messenger.compose.view.profiles.Profiles
import de.connect2x.messenger.compose.view.profiles.ShowProfileCreation
import de.connect2x.messenger.compose.view.profiles.WithProfileSelection
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMultiMessengerServiceConnection
import de.connect2x.trixnity.messenger.compose.view.R
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.defaultActivityGetter
import de.connect2x.trixnity.messenger.util.defaultSharedDataHandler
import de.connect2x.trixnity.messenger.util.defaultUriHandler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MatrixMultiMessengerActivity : AppCompatActivity() {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.MatrixMultiMessengerActivity")
    private val matrixMultiMessengerServiceConnection: MatrixMultiMessengerServiceConnection =
        MatrixMultiMessengerServiceConnection()
    private val coroutineScope: CoroutineScope =
        CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, exception ->
            log.error(exception) { "Exception in MatrixMultiMessengerActivity coroutine" }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        log.debug { "Creating activity instance for '${getString(R.string.app_name)}'" }

        matrixMultiMessengerServiceConnection.bind(applicationContext)
        onNewIntent(intent)

        checkExternalStoragePermissions()

        val componentContext = defaultComponentContext()

        coroutineScope.launch {
            val matrixMultiMessenger =
                matrixMultiMessengerServiceConnection.instance.filterNotNull().first()
            matrixMultiMessenger.defaultActivityGetter { this@MatrixMultiMessengerActivity }
            val backHandler = matrixMultiMessenger.di.get<BackHandler>()
            onBackPressedDispatcher.addCallback {
                backHandler.goBack()
            }
            withContext(Dispatchers.Main) {
                setContent {
                    WithProfileSelection(
                        matrixMultiMessenger = matrixMultiMessenger,
                        componentContext = componentContext,
                        activeMessengerOnce = { _, _ -> },
                        activeMessenger = { matrixMessenger, rootViewModel ->
                            val isFocusHighlighting =
                                matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                                    .collectAsState().value.base.isFocusHighlighting
                            CompositionLocalProvider(
                                Platform provides PlatformType.ANDROID,
                                DI provides matrixMessenger.di,
                                IsFocusHighlighting provides isFocusHighlighting,
                            ) {
                                MessengerTheme {
                                    Client(rootViewModel)
                                }
                            }
                        }
                    ) {
                        val showProfileCreation = remember { mutableStateOf(false) }
                        CompositionLocalProvider(
                            Platform provides PlatformType.ANDROID,
                            DI provides matrixMultiMessenger.di,
                            ShowProfileCreation provides showProfileCreation,
                            IsFocusHighlighting provides false,
                        ) {
                            MessengerTheme {
                                Profiles()
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

    override fun onStop() {
        log.debug { "Stopping activity" }
        super.onStop()
    }

    override fun onStart() {
        log.debug { "Starting activity" }
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        matrixMultiMessengerServiceConnection.unbind(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        coroutineScope.launch {
            val multiMessenger = matrixMultiMessengerServiceConnection.instance.filterNotNull().first()
            when (intent.action) {
                Intent.ACTION_VIEW -> intent.data?.also {
                    multiMessenger.defaultUriHandler.onUri(it)
                }

                Intent.ACTION_SEND if intent.type == "text/plain" -> {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        val uri = text.toUri()

                        val sharedIntentData =
                            if (uri.scheme == "http" || uri.scheme == "https") {
                                val icon = intent.clipData?.toSequence()?.firstOrNull()?.uri

                                SharedIntentData.SharedUrl(text, icon)
                            } else {
                                SharedIntentData.SharedText(text)
                            }
                        val i18n = multiMessenger.di.get<I18n>()
                        multiMessenger.defaultSharedDataHandler.onShare(
                            sharedIntentData.toSharedData(applicationContext, i18n)
                        )
                    }
                }

                Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> intent.clipData?.let { clipData ->
                    val i18n = multiMessenger.di.get<I18n>()
                    multiMessenger.defaultSharedDataHandler.onShare(
                        clipData.toList().let(SharedIntentData::SharedItems).toSharedData(applicationContext, i18n)
                    )
                }
            }
        }
    }
}
