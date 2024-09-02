package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.initialsync.AccountSync
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

interface SyncOverlayView {
    @Composable
    fun create(syncViewModel: SyncViewModel)
}

@Composable
fun SyncOverlay(syncViewModel: SyncViewModel) {
    DI.current.get<SyncOverlayView>().create(syncViewModel)
}

class SyncOverlayViewImpl : SyncOverlayView {
    @Composable
    override fun create(syncViewModel: SyncViewModel) {

        val i18n = DI.current.get<I18nView>()
        val accountSyncStates by syncViewModel.accountSyncStates.collectAsState()
        val showAbortButton = remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(5.seconds)
            showAbortButton.value = true
        }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer).clickable { }) {
            Box(Modifier.align(Alignment.Center).padding(20.dp)) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .sizeIn(maxWidth = 500.dp)
                ) {
                    Column(
                        Modifier
                            .align(Alignment.Center)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
                            Text(
                                i18n.syncOverlayTitle(),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            accountSyncStates?.entries?.map { (userId, accountSync) ->
                                Spacer(Modifier.size(20.dp))
                                when (accountSync) {
                                    AccountSync.INITIAL_SYNC -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1.0f, fill = true)) {
                                                Text(
                                                    i18n.syncOverlayAccount(userId),
                                                    style = MaterialTheme.typography.titleSmall,
                                                )
                                                Text(i18n.syncOverlayInitialSync())
                                                Text(
                                                    text = i18n.syncOverlayInitialSyncInfo(DI.current.get<MatrixMessengerConfiguration>().appName),
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            Spacer(Modifier.size(20.dp))
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 20.dp),
                                            )
                                        }
                                    }

                                    AccountSync.SYNC -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                userId.full,
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.weight(1.0f, fill = true),
                                            )
                                            Spacer(Modifier.size(20.dp))
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 20.dp),
                                            )
                                        }
                                    }

                                    AccountSync.DONE -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                userId.full,
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.weight(1.0f, fill = true)
                                            )
                                            Spacer(Modifier.size(20.dp))
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                i18n.commonDone(),
                                                Modifier.padding(end = 25.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (showAbortButton.value) {
                            Spacer(Modifier.size(20.dp))
                            Button(
                                onClick = syncViewModel::cancel,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.buttonPointerModifier(),
                            ) {
                                Text(i18n.commonCancel().capitalize(Locale.current))
                            }
                        }
                    }
                }
            }
        }
    }
}